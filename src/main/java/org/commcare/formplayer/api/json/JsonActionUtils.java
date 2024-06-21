package org.commcare.formplayer.api.json;

import org.commcare.formplayer.api.util.ApiConstants;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.AnswerDataFactory;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.form.api.FormEntryPrompt;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import datadog.trace.api.Trace;

/**
 * Utility functions for performing some action on a Form and receiving a JSON response
 */
public class JsonActionUtils {

    /**
     * Delete a repeat at the specified index, return the JSON response
     *
     * @param controller      the FormEntryController under consideration
     * @param model           the FormEntryModel under consideration
     * @param repeatIndexString the form index of the repeat group to be deleted
     * @return The JSON representation of the updated form tree
     */
    public static JSONObject deleteRepeatToJson(FormEntryController controller,
            FormEntryModel model, String repeatIndexString) {
        FormIndex indexToDelete = indexFromString(repeatIndexString, model.getForm());
        controller.deleteRepeat(indexToDelete);
        return getCurrentJson(controller, model);
    }

    /**
     * Expand (IE create) the repeat at the specified form index
     *
     * @param controller      the FormEntryController under consideration
     * @param model           the FormEntryModel under consideration
     * @param formIndexString the form index of the repeat group to be expanded
     * @return The JSON representation of the updated question tree
     */
    public static JSONObject descendRepeatToJson(FormEntryController controller,
            FormEntryModel model, String formIndexString) {
        FormIndex formIndex = indexFromString(formIndexString, model.getForm());
        controller.jumpToIndex(formIndex);
        controller.descendIntoNewRepeat();
        return getCurrentJson(controller, model);
    }

    /**
     * Get the JSON representation of the question tree of this controller/model pair
     *
     * @param controller the FormEntryController under consideration
     * @param model      the FormEntryModel under consideration
     * @return The JSON representation of the question tree
     */
    public static JSONObject getCurrentJson(FormEntryController controller,
            FormEntryModel model) {
        JSONObject ret = new JSONObject();
        ret.put(ApiConstants.QUESTION_TREE_KEY, getFullFormJSON(model, controller));
        return ret;
    }


    // Similar to above, but get the questions for only one formIndex (OQPS)
    public static JSONObject getCurrentJson(FormEntryController controller,
            FormEntryModel model,
            String formIndex) {
        return new JSONObject().put(ApiConstants.QUESTION_TREE_KEY,
                getOneQuestionPerScreenJson(model, controller,
                        JsonActionUtils.indexFromString(formIndex, model.getForm())));
    }

    // Similar to above, but get the questions for only one formIndex (OQPS)
    public static JSONObject getPromptJson(FormEntryModel model) {
        return new JSONObject().put(ApiConstants.QUESTION_EVENT_KEY,
                PromptToJson.parseQuestionType(model, new JSONObject()));
    }

    /**
     * Answer the question, return the updated JSON representation of the question tree
     *
     * @param controller the FormEntryController under consideration
     * @param model      the FormEntryModel under consideration
     * @param answer     the answer to enter
     * @param prompt     the question to be answered
     * @return The JSON representation of the updated question tree
     */
    @Trace
    private static JSONObject questionAnswerToJson(FormEntryController controller,
            FormEntryModel model, String answer,
            FormEntryPrompt prompt,
            boolean oneQuestionPerScreen,
            FormIndex navIndex,
            boolean skipValidation,
            boolean returnTree) {
        JSONObject ret = new JSONObject();
        IAnswerData answerData;
        int result;

        if (answer == null) {
            answerData = null;
        } else {
            try {
                answerData = getAnswerData(prompt, answer);
            } catch (IllegalArgumentException e) {
                ret.put(ApiConstants.RESPONSE_STATUS_KEY, "error");
                ret.put(ApiConstants.ERROR_TYPE_KEY, "illegal-argument");
                ret.put(ApiConstants.ERROR_REASON_KEY, e.getMessage());
                return ret;
            }
        }

        IAnswerData currentAnswerData = model.getForm().getInstance().resolveReference(
                prompt.getIndex().getReference()).getValue();
        if (skipValidation && answerData != null && currentAnswerData != null
                && answerData.uncast().getValue().equals(currentAnswerData.uncast().getValue())) {
            result = FormEntryController.ANSWER_OK;
        } else {
            result = controller.answerQuestion(prompt.getIndex(), answerData);
        }

        if (result == FormEntryController.ANSWER_REQUIRED_BUT_EMPTY) {
            ret.put(ApiConstants.RESPONSE_STATUS_KEY, "validation-error");
            ret.put(ApiConstants.ERROR_TYPE_KEY, "required");
        } else if (result == FormEntryController.ANSWER_CONSTRAINT_VIOLATED) {
            ret.put(ApiConstants.RESPONSE_STATUS_KEY, "validation-error");
            ret.put(ApiConstants.ERROR_TYPE_KEY, "constraint");
            ret.put(ApiConstants.ERROR_REASON_KEY, prompt.getConstraintText());
        } else if (result == FormEntryController.ANSWER_OK) {
            if (returnTree) {
                if (oneQuestionPerScreen) {
                    ret.put(ApiConstants.QUESTION_TREE_KEY, getOneQuestionPerScreenJson(
                            model, controller, navIndex));
                } else {
                    ret.put(ApiConstants.QUESTION_TREE_KEY, getFullFormJSON(model, controller));
                }
            }
            ret.put(ApiConstants.RESPONSE_STATUS_KEY, "accepted");
        }
        return ret;
    }

    /**
     * Answer the question, return the updated JSON representation of the question tree
     *
     * @param controller the FormEntryController under consideration
     * @param model      the FormEntryModel under consideration
     * @param answer     the answer to enter
     * @param ansIndex   the form index of the question to be answered
     * @return The JSON representation of the updated question tree
     */
    public static JSONObject questionAnswerToJson(FormEntryController controller,
            FormEntryModel model, String answer,
            String ansIndex,
            boolean oneQuestionPerScreen,
            String navIndex,
            boolean skipValidation,
            boolean returnTree) {
        FormIndex answerIndex = indexFromString(ansIndex, model.getForm());
        FormEntryPrompt prompt = model.getQuestionPrompt(answerIndex);
        FormIndex navigationIndex = indexFromString(navIndex, model.getForm());
        return questionAnswerToJson(controller, model, answer, prompt, oneQuestionPerScreen,
                navigationIndex, skipValidation, returnTree);
    }

    /**
     * Return the IAnswerData version of the string data input
     *
     * @param formEntryPrompt the FormEntryPrompt for this question
     * @param data            the String answer
     * @return the IAnswerData version of @data above
     */
    @Trace
    private static IAnswerData getAnswerData(FormEntryPrompt formEntryPrompt, String data) {
        int index;
        switch (formEntryPrompt.getDataType()) {
            case Constants.DATATYPE_CHOICE:
                index = Integer.parseInt(data);
                SelectChoice selectChoiceAnswer = formEntryPrompt.getSelectChoices().get(index - 1);
                return new SelectOneData(selectChoiceAnswer.selection());
            case Constants.DATATYPE_CHOICE_LIST:
                String[] split = parseMultiSelectString(data);
                Vector<Selection> ret = new Vector<>();
                for (String s : split) {
                    index = Integer.parseInt(s);
                    Selection selection = formEntryPrompt.getSelectChoices().get(
                            index - 1).selection();
                    ret.add(selection);
                }
                return new SelectMultiData(ret);
            case Constants.DATATYPE_GEOPOINT:
                return AnswerDataFactory.template(formEntryPrompt.getControlType(),
                        formEntryPrompt.getDataType()).cast(
                        new UncastData(convertTouchFormsGeoPointString(data)));
        }
        return data.equals("") ? null : AnswerDataFactory.template(formEntryPrompt.getControlType(),
                formEntryPrompt.getDataType()).cast(new UncastData(data));
    }

    // we need to remove the brackets Touchforms includes and replace the commas with spaces
    private static String convertTouchFormsGeoPointString(String touchformsString) {
        return touchformsString.replace(",", " ").replace("[", "").replace("]", "");
    }

    /**
     * OK, this function is kind of a monster. Given a FormDef and a String representation of the
     * form index, return a full fledged FormIndex object.
     */
    public static FormIndex indexFromString(String stringIndex, FormDef form) {
        if (stringIndex == null || stringIndex.equals("None")) {
            return null;
        } else if (stringIndex.equals("<")) {
            return FormIndex.createBeginningOfFormIndex();
        } else if (stringIndex.equals(">")) {
            return FormIndex.createEndOfFormIndex();
        }

        List<Pair<Integer, Integer>> list = stepToList(stringIndex);

        FormIndex ret = reduceFormIndex(list, null);
        ret.assignRefs(form);
        return ret;
    }

    public static int getQuestionType(FormEntryModel model, String stringIndex, FormDef form) {
        FormIndex index = indexFromString(stringIndex, form);
        return model.getEvent(index);
    }

    /**
     * Given a String represnetation of a form index, decompose it into a list of index,
     * multiplicity pairs
     *
     * @param index the comma separated String representation of the form index
     * @return @index represented as a list of index,multiplicity integer pairs
     */
    private static List<Pair<Integer, Integer>> stepToList(String index) {
        ArrayList<Pair<Integer, Integer>> ret = new ArrayList<>();
        String[] split = index.split(",");
        List<String> list = Arrays.asList(split);
        Collections.reverse(list);
        for (String step : list) {
            if (!step.trim().equals("")) {
                Pair<Integer, Integer> pair = stepFromString(step);
                ret.add(pair);
            }
        }
        return ret;
    }

    /**
     * Given the string representation of one "Step" in a form index, return an Integer pair of
     * index, multiplicity
     */
    private static Pair<Integer, Integer> stepFromString(String step) {
        // honestly not sure. thanks obama/drew
        if (step.endsWith("J")) {
            return new Pair<>(Integer.parseInt("" + step.substring(0, step.length() - 1)),
                    TreeReference.INDEX_REPEAT_JUNCTURE);
        }
        // we want to deal with both '.' and '_' as separators for the time being for TF legacy
        // reasons
        String[] split = step.split("[._:]");
        // the form index is the first part, the multiplicity is the second
        int i = Integer.parseInt(split[0].trim());
        int mult = -1;
        if (split.length > 1 && split[1] != null) {
            mult = Integer.parseInt(split[1].trim());
        }
        return new Pair<>(i, mult);
    }

    /**
     * Given a list of steps (see above) to be traversed and a current Form index, pop the top step
     * and create a new FormIndex from this step with the current as its parent, then recursively
     * call this function with the remaining steps and the new FormIndex
     */
    private static FormIndex reduceFormIndex(List<Pair<Integer, Integer>> steps,
            FormIndex current) {
        if (steps.size() == 0) {
            return current;
        }
        Pair<Integer, Integer> currentStep = steps.remove(0);
        FormIndex nextLevel = new FormIndex(current, currentStep.first, currentStep.second, null);
        return reduceFormIndex(steps, nextLevel);
    }

    private static String[] parseMultiSelectString(String answer) {
        answer = answer.trim();
        if (answer.startsWith("[") && answer.endsWith("]")) {
            answer = answer.substring(1, answer.length() - 1);
        }
        String[] ret = answer.split(" ");
        for (int i = 0; i < ret.length; i++) {
            ret[i] = ret[i].replace(",", "");
        }
        return ret;
    }

    @Trace
    public static JSONArray getFullFormJSON(FormEntryModel fem, FormEntryController fec) {
        JSONArray ret = new JSONArray();
        Walker walker = new Walker(ret, FormIndex.createBeginningOfFormIndex(), fec, fem);
        walker.walk();
        return ret;
    }

    public static JSONArray getOneQuestionPerScreenJson(FormEntryModel fem, FormEntryController fec,
            FormIndex formIndex) {
        JSONArray ret = new JSONArray();
        if (formIndex.isEndOfFormIndex()) {
            return ret;
        }
        FormEntryPrompt[] prompts = fec.getQuestionPrompts(formIndex);
        for (FormEntryPrompt prompt : prompts) {
            fem.setQuestionIndex(prompt.getIndex());
            JSONObject obj = new JSONObject();
            PromptToJson.parseQuestionType(fem, obj);
            ret.put(obj);
        }
        return ret;
    }
}
