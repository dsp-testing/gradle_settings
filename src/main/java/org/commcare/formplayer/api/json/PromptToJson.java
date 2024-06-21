package org.commcare.formplayer.api.json;

import org.commcare.formplayer.exceptions.ApplicationConfigException;
import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.GroupDef;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.form.api.FormEntryPrompt;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.Vector;

import datadog.trace.api.Trace;

/**
 * Functions for generating the JSON repesentation of a FormEntryPrompt
 *
 * @author wspride
 */
public class PromptToJson {

    /**
     * @param prompt       The FormEntryPrompt under consideration
     * @param questionJson the JSON object question representation being generated
     */
    public static void parseQuestion(FormEntryPrompt prompt, JSONObject questionJson) {
        parseCaption(prompt, questionJson);
        questionJson.put("help", jsonNullIfNull(prompt.getHelpText()));
        TreeReference questionRef = prompt.getQuestion().getBind().getReference();
        questionJson.put("binding", jsonNullIfNull(questionRef.toString()));
        questionJson.put("question_id", jsonNullIfNull(questionRef.getNameLast()));
        questionJson.put("datatype", jsonNullIfNull(parseDataType(prompt)));
        questionJson.put("control", jsonNullIfNull(prompt.getControlType()));
        questionJson.put("required", prompt.isRequired() ? 1 : 0);
        questionJson.put("hint", jsonNullIfNull(prompt.getHintText()));
        parseQuestionAnswer(questionJson, prompt);
        questionJson.put("ix", jsonNullIfNull(prompt.getIndex()));

        questionJson.put("help_image", jsonNullIfNull(prompt.getHelpMultimedia(FormEntryCaption.TEXT_FORM_IMAGE)));
        questionJson.put("help_audio", jsonNullIfNull(prompt.getHelpMultimedia(FormEntryCaption.TEXT_FORM_AUDIO)));
        questionJson.put("help_video", jsonNullIfNull(prompt.getHelpMultimedia(FormEntryCaption.TEXT_FORM_VIDEO)));

        if (prompt.getDataType() == Constants.DATATYPE_CHOICE
                || prompt.getDataType() == Constants.DATATYPE_CHOICE_LIST) {
            questionJson.put("choices", parseSelect(prompt));
        }
    }

    /**
     * @param prompt       The FormEntryCaption to get the caption fields from
     * @param questionJson The JSON question representation being built
     */
    public static void parseCaption(FormEntryCaption prompt, JSONObject questionJson) {
        questionJson.put("caption_audio", jsonNullIfNull(prompt.getAudioText()));
        questionJson.put("caption", jsonNullIfNull(prompt.getLongText()));
        questionJson.put("caption_image", jsonNullIfNull(prompt.getImageText()));
        questionJson.put("caption_video", jsonNullIfNull(prompt.getVideoText()));
        questionJson.put("caption_markdown", jsonNullIfNull(prompt.getMarkdownText()));
    }

    // We want to use the JSONObject null if the object is null, not the Java null
    public static Object jsonNullIfNull(Object obj) {
        return obj == null ? JSONObject.NULL : obj;
    }

    public static JSONObject parseQuestionType(FormEntryModel model, JSONObject obj) {
        int status = model.getEvent();
        FormIndex ix = model.getFormIndex();
        obj.put("ix", ix.toString());

        switch (status) {
            case FormEntryController.EVENT_BEGINNING_OF_FORM:
                obj.put("type", "form-start");
                break;
            case FormEntryController.EVENT_END_OF_FORM:
                obj.put("ix", ">");
                obj.put("type", "form-complete");
                break;
            case FormEntryController.EVENT_QUESTION:
                obj.put("type", "question");
                obj.put("style", jsonNullIfNull(parseStyle(model.getCaptionPrompt())));
                parseQuestion(model.getQuestionPrompt(), obj);
                break;
            case FormEntryController.EVENT_REPEAT_JUNCTURE:
                obj.put("type", "repeat-juncture");
                parseRepeatJuncture(model, obj, ix);
                break;
            case FormEntryController.EVENT_GROUP:
                // we're in a subgroup
                parseCaption(model.getCaptionPrompt(), obj);
                obj.put("type", "sub-group");
                obj.put("style", jsonNullIfNull(parseStyle(model.getCaptionPrompt())));
                obj.put("repeatable", false);
                break;
            case FormEntryController.EVENT_REPEAT:
                // we're in a subgroup
                parseCaption(model.getCaptionPrompt(), obj);
                obj.put("type", "sub-group");
                obj.put("repeatable", true);
                obj.put("exists", true);
                obj.put("delete", model.isNonCountedRepeat());
                break;
            case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
                // we're in a subgroup, dummy node for user counted repeat group
                FormEntryCaption prompt = model.getCaptionPrompt();
                parseCaption(prompt, obj);
                obj.put("type", "sub-group");
                obj.put("repeatable", true);
                obj.put("exists", false);
                obj.put("delete", false);
                obj.put("add-choice", getRepeatAddText(prompt));
                break;
        }
        return obj;
    }

    private static String getRepeatAddText(FormEntryCaption prompt) {
        String promptText = prompt.getLongText();
        if (prompt.getNumRepetitions() > 0) {
            try {
                return Localization.get("repeat.dialog.add.another", promptText);
            } catch (NoLocalizedTextException e) {
                return "Add another " + promptText;
            }
        } else {
            try {
                return Localization.get("repeat.dialog.add.new", promptText);
            } catch (NoLocalizedTextException e) {
                return "Add a new " + promptText;
            }
        }
    }

    private static void parseRepeatJuncture(FormEntryModel model, JSONObject obj, FormIndex ix) {
        FormEntryCaption formEntryCaption = model.getCaptionPrompt(ix);
        FormEntryCaption.RepeatOptions repeatOptions = formEntryCaption.getRepeatOptions();
        parseCaption(formEntryCaption, obj);
        obj.put("header", repeatOptions.header);
        obj.put("repetitions", new JSONArray(formEntryCaption.getRepetitionsText()));
        obj.put("add-choice", repeatOptions.add);
        obj.put("delete-choice", repeatOptions.delete);
        obj.put("del-header", repeatOptions.delete_header);
        obj.put("done-choice", repeatOptions.done);
    }

    @Trace
    private static void parseQuestionAnswer(JSONObject obj, FormEntryPrompt prompt) {
        IAnswerData answerValue = prompt.getAnswerValue();
        if (answerValue == null) {
            obj.put("answer", JSONObject.NULL);
            return;
        }
        switch (prompt.getDataType()) {
            case Constants.DATATYPE_NULL:
            case Constants.DATATYPE_BARCODE:
            case Constants.DATATYPE_TEXT:
                obj.put("answer", answerValue.getDisplayText());
                return;
            case Constants.DATATYPE_INTEGER:
                obj.put("answer", (int)answerValue.getValue());
                return;
            case Constants.DATATYPE_LONG:
                obj.put("answer", (long)answerValue.getValue());
                return;
            case Constants.DATATYPE_DECIMAL:
                obj.put("answer", (double)answerValue.getValue());
                return;
            case Constants.DATATYPE_DATE:
                obj.put("answer", (DateUtils.formatDate((Date)answerValue.getValue(),
                        DateUtils.FORMAT_ISO8601)));
                return;
            case Constants.DATATYPE_TIME:
                obj.put("answer", answerValue.getDisplayText());
                return;
            case Constants.DATATYPE_DATE_TIME:
                DateTime answer = new DateTime(answerValue.getValue());
                obj.put("answer", answer.toString("yyyy-MM-dd'T'HH:mm:ssZZ"));
                return;
            case Constants.DATATYPE_CHOICE:
                Selection singleSelection = ((Selection)answerValue.getValue());
                singleSelection.attachChoice(prompt.getQuestion());
                int singleOrdinal = singleSelection.getTouchformsIndex();
                if (singleOrdinal > 0) {
                    obj.put("answer", singleOrdinal);
                }
                return;
            case Constants.DATATYPE_CHOICE_LIST:
                Vector<Selection> selections = ((SelectMultiData)answerValue).getValue();
                JSONArray acc = new JSONArray();
                for (Selection selection : selections) {
                    selection.attachChoice(prompt.getQuestion());
                    int multiOrdinal = selection.getTouchformsIndex();
                    if (multiOrdinal > 0) {
                        acc.put(multiOrdinal);
                    }
                }
                obj.put("answer", acc);
                return;
            case Constants.DATATYPE_GEOPOINT:
                GeoPointData geoPointData = ((GeoPointData)prompt.getAnswerValue());
                double[] coords =
                        new double[]{geoPointData.getLatitude(), geoPointData.getLongitude()};
                obj.put("answer", new JSONArray(Arrays.toString(coords)));
                return;
            case Constants.DATATYPE_BINARY:
                obj.put("answer", answerValue.getDisplayText());
        }
    }

    /**
     * Given a prompt, generate a JSONArray of the possible select choices. return empty array if no
     * choices.
     */
    private static JSONArray parseSelect(FormEntryPrompt prompt) {
        JSONArray obj = new JSONArray();
        for (SelectChoice choice : prompt.getSelectChoices()) {
            String choiceValue = prompt.getSelectChoiceText(choice);
            if (prompt.getControlType() == Constants.CONTROL_SELECT_MULTI
                    && choice.getValue().contains(" ")) {
                throw new ApplicationConfigException(
                        String.format("Select answer options cannot contain spaces. " +
                                "Question %s with answer %s", prompt, choiceValue));
            }
            obj.put(prompt.getSelectChoiceText(choice));
        }
        return obj;
    }

    private static JSONObject parseStyle(FormEntryCaption caption) {
        String hint = caption.getAppearanceHint();
        if (hint == null) {
            return null;
        }
        JSONObject ret = new JSONObject().put("raw", hint);
        return ret;
    }


    private static String parseDataType(FormEntryPrompt prompt) {
        if (prompt.getControlType() == Constants.CONTROL_TRIGGER) {
            return "info";
        }
        switch (prompt.getDataType()) {
            case Constants.DATATYPE_NULL:
                // Default to treating nodes with null data type as strings
            case Constants.DATATYPE_TEXT:
                return "str";
            case Constants.DATATYPE_INTEGER:
                return "int";
            case Constants.DATATYPE_LONG:
                return "longint";
            case Constants.DATATYPE_DECIMAL:
                return "float";
            case Constants.DATATYPE_DATE:
                return "date";
            case Constants.DATATYPE_TIME:
                return "time";
            case Constants.DATATYPE_CHOICE:
                return "select";
            case Constants.DATATYPE_CHOICE_LIST:
                return "multiselect";
            case Constants.DATATYPE_GEOPOINT:
                return "geo";
            case Constants.DATATYPE_DATE_TIME:
                return "datetime";
            case Constants.DATATYPE_BARCODE:
                return "barcode";
            case Constants.DATATYPE_BINARY:
                return "binary";
        }
        return "unrecognized";
    }
}
