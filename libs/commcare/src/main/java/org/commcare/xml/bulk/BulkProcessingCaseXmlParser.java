package org.commcare.xml.bulk;

import static org.commcare.xml.CaseXmlParserUtil.CASE_ATTACHMENT_NODE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_CLOSE_NODE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_CREATE_NODE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_INDEX_NODE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_NODE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_ATTACHMENT_FROM;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_ATTACHMENT_NAME;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_ATTACHMENT_SRC;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_CASE_ID;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_CASE_NAME;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_CASE_TYPE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_CATEGORY;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_DATE_MODIFIED;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_DATE_OPENED;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_EXTERNAL_ID;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_OWNER_ID;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_STATE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_USER_ID;
import static org.commcare.xml.CaseXmlParserUtil.CASE_UPDATE_NODE;
import static org.commcare.xml.CaseXmlParserUtil.getTrimmedElementTextOrBlank;
import static org.commcare.xml.CaseXmlParserUtil.indexCase;
import static org.commcare.xml.CaseXmlParserUtil.validateMandatoryProperty;

import org.commcare.cases.model.Case;
import org.commcare.xml.CaseIndexChangeListener;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.util.externalizable.SerializationLimitationException;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A parser which is capable of processing CaseXML transactions in a bulk format.
 *
 * This parser needs an implementation which can perform the "bulk" steps efficiently on the current
 * platform.
 *
 * It should be a drop-in replacement for the CaseXmlParser in the ways that it is used
 *
 * Created by ctsims on 3/14/2017.
 */
public abstract class BulkProcessingCaseXmlParser extends BulkElementParser<Case> implements
        CaseIndexChangeListener {

    public BulkProcessingCaseXmlParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    protected void requestModelReadsForElement(TreeElement bufferedTreeElement, Set<String> currentBulkReadSet) {
        String caseId = bufferedTreeElement.getAttributeValue(null, "case_id");
        currentBulkReadSet.add(caseId);
    }

    @Override
    protected void preParseValidate() throws InvalidStructureException {
        checkNode(CASE_NODE);
    }

    @Override
    protected void processBufferedElement(TreeElement bufferedTreeElement, Map<String, Case> currentOperatingSet, LinkedHashMap<String, Case> writeLog) throws InvalidStructureException {
        String caseId = bufferedTreeElement.getAttributeValue(null, CASE_PROPERTY_CASE_ID);
        validateMandatoryProperty(CASE_PROPERTY_CASE_ID, caseId, "", parser);

        String dateModified = bufferedTreeElement.getAttributeValue(null, CASE_PROPERTY_DATE_MODIFIED);
        validateMandatoryProperty(CASE_PROPERTY_DATE_MODIFIED, dateModified, caseId, parser);
        Date modified = DateUtils.parseDateTime(dateModified);

        String userId = parser.getAttributeValue(null, CASE_PROPERTY_USER_ID);

        Case caseForBlock = null;
        boolean isCreateOrUpdate = false;

        for (int i = 0; i < bufferedTreeElement.getNumChildren(); i++) {
            TreeElement subElement = bufferedTreeElement.getChildAt(i);
            String action = subElement.getName().toLowerCase();
            switch (action) {
                case CASE_CREATE_NODE:
                    caseForBlock = createCase(subElement, currentOperatingSet, caseId, modified, userId);
                    isCreateOrUpdate = true;
                    break;
                case CASE_UPDATE_NODE:
                    caseForBlock = loadCase(caseForBlock, caseId, currentOperatingSet, true);
                    updateCase(subElement, caseForBlock, caseId);
                    isCreateOrUpdate = true;
                    break;
                case CASE_CLOSE_NODE:
                    caseForBlock = loadCase(caseForBlock, caseId, currentOperatingSet, true);
                    closeCase(caseForBlock, caseId);
                    break;
                case CASE_INDEX_NODE:
                    caseForBlock = loadCase(caseForBlock, caseId, currentOperatingSet, false);
                    indexCase(subElement, caseForBlock, caseId, this);
                    break;
                case CASE_ATTACHMENT_NODE:
                    caseForBlock = loadCase(caseForBlock, caseId, currentOperatingSet, false);
                    processCaseAttachment(subElement, caseForBlock);
                    break;
            }

        }

        if (caseForBlock != null) {
            caseForBlock.setLastModified(modified);

            try {
                writeLog.put(caseForBlock.getCaseId(), caseForBlock);
                currentOperatingSet.put(caseForBlock.getCaseId(), caseForBlock);
            } catch (SerializationLimitationException e) {
                throw new InvalidStructureException("One of the property values for the case named '" +
                        caseForBlock.getName() + "' is too large (by " + e.percentOversized +
                        "%). Please show your supervisor.");
            }

            if (isCreateOrUpdate) {
                onCaseCreateUpdate(caseId);
            }
        }
    }


    private Case createCase(TreeElement createElement, Map<String, Case> currentOperatingSet,
                            String caseId, Date modified, String userId) throws InvalidStructureException {

        String[] data = new String[3];
        Case caseForBlock;

        for (int i = 0; i < createElement.getNumChildren(); i++) {
            TreeElement subElement = createElement.getChildAt(i);
            String tag = subElement.getName();
            switch (tag) {
                case CASE_PROPERTY_CASE_TYPE:
                    data[0] = getTrimmedElementTextOrBlank(subElement);
                    break;
                case CASE_PROPERTY_OWNER_ID:
                    data[1] = getTrimmedElementTextOrBlank(subElement);
                    break;
                case CASE_PROPERTY_CASE_NAME:
                    data[2] = getTrimmedElementTextOrBlank(subElement);
                    break;
                default:
                    throw new InvalidStructureException("Expected one of [case_type, owner_id, case_name], found " + tag);
            }
        }

        if (data[0] == null || data[2] == null) {
            throw new InvalidStructureException("One of [case_type, case_name] is missing for case <create> with ID: " + caseId);
        }

        caseForBlock = currentOperatingSet.get(caseId);

        if (caseForBlock != null) {
            caseForBlock.setName(data[2]);
            caseForBlock.setTypeId(data[0]);
        }

        if (caseForBlock == null) {
            // The case is either not present on the phone, or we're on strict tolerance
            caseForBlock = buildCase(data[2], data[0]);
            caseForBlock.setCaseId(caseId);
            caseForBlock.setDateOpened(modified);
        }

        if (data[1] != null) {
            caseForBlock.setUserId(data[1]);
        } else {
            caseForBlock.setUserId(userId);
        }
        if (caseForBlock.getUserId() == null || caseForBlock.getUserId().contentEquals("")) {
            throw new InvalidStructureException("One of [user_id, owner_id] is missing for case <create> with ID: " + caseId, parser);
        }
        return caseForBlock;
    }

    protected Case buildCase(String name, String typeId) {
        return new Case(name, typeId);
    }

    private Case loadCase(Case caseForBlock, String caseId, Map<String, Case> currentOperatingSet,
                          boolean errorIfMissing) throws InvalidStructureException {
        if (caseForBlock == null) {
            caseForBlock = currentOperatingSet.get(caseId);
        }
        if (errorIfMissing && caseForBlock == null) {
            throw new InvalidStructureException("Unable to update or close case " + caseId + ", it wasn't found");
        }
        return caseForBlock;
    }

    private void updateCase(TreeElement updateElement,
                            Case caseForBlock, String caseId) {

        for (int i = 0; i < updateElement.getNumChildren(); i++) {
            TreeElement subElement = updateElement.getChildAt(i);

            String key = subElement.getName();
            String value = getTrimmedElementTextOrBlank(subElement);

            switch (key) {
                case CASE_PROPERTY_CASE_TYPE:
                    caseForBlock.setTypeId(value);
                    break;
                case CASE_PROPERTY_CASE_NAME:
                    caseForBlock.setName(value);
                    break;
                case CASE_PROPERTY_DATE_OPENED:
                    caseForBlock.setDateOpened(DateUtils.parseDate(value));
                    break;
                case CASE_PROPERTY_OWNER_ID:
                    String oldUserId = caseForBlock.getUserId();

                    if (!oldUserId.equals(value)) {
                        onIndexDisrupted(caseId);
                    }
                    caseForBlock.setUserId(value);
                    break;
                case CASE_PROPERTY_EXTERNAL_ID:
                    caseForBlock.setExternalId(value);
                    break;
                case CASE_PROPERTY_CATEGORY:
                    caseForBlock.setCategory(value);
                    break;
                case CASE_PROPERTY_STATE:
                    caseForBlock.setState(value);
                    break;
                default:
                    caseForBlock.setProperty(key, value);
                    break;
            }
        }
    }

    private void closeCase(Case caseForBlock, String caseId) {
        caseForBlock.setClosed(true);
        //this used to insist on a write happening _right here_. Not sure exactly why. Maybe related
        //to other writes happening in the same restore?
        onIndexDisrupted(caseId);
    }

    @Override
    public void onIndexDisrupted(String caseId) {

    }

    protected void onCaseCreateUpdate(String caseId) {

    }

    //These are unlikely to be used, and likely need to be refactored still a bit

    private void processCaseAttachment(TreeElement attachmentElement, Case caseForBlock) {
        for (int i = 0; i < attachmentElement.getNumChildren(); i++) {
            TreeElement subElement = attachmentElement.getChildAt(i);

            String attachmentName = subElement.getName();
            String src = subElement.getAttributeValue(null, CASE_PROPERTY_ATTACHMENT_SRC);
            String from = subElement.getAttributeValue(null, CASE_PROPERTY_ATTACHMENT_FROM);
            String fileName = subElement.getAttributeValue(null, CASE_PROPERTY_ATTACHMENT_NAME);

            if ((src == null || "".equals(src)) && (from == null || "".equals(from))) {
                //this is actually an attachment removal
                removeAttachment(caseForBlock, attachmentName);
                caseForBlock.removeAttachment(attachmentName);
                continue;
            }

            String reference = processAttachment(src, from, fileName);
            if (reference != null) {
                caseForBlock.updateAttachment(attachmentName, reference);
            }
        }
    }

    protected void removeAttachment(Case caseForBlock, String attachmentName) {
        throw new RuntimeException("Attachment processing not available for bulk reads");
    }

    protected String processAttachment(String src, String from, String name) {
        throw new RuntimeException("Attachment processing not available for bulk reads");
    }
}
