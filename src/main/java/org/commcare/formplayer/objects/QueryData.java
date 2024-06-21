package org.commcare.formplayer.objects;

import com.fasterxml.jackson.annotation.*;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import java.util.Hashtable;
import java.util.Map;


/**
 * Created by jschweers on 12/28/20.
 *
 * QueryData stores the case search & claim data for a session.
 * It's a hashtable keyed by a unique key combination of command id
 * and query datum id, e.g., "search_command.m2_results"
 * For each command, QueryData stores two values:
 *  1. A boolean "execute" flag. If true, the search should be run.
 *     If not, just fetch the current values of the prompts.
 *  2. An "inputs" map where keys are field names and values are
 *     search terms.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryData extends Hashtable<String, Object> {
    private static final String KEY_EXECUTE = "execute";
    public static final String KEY_FORCE_MANUAL_SEARCH = "force_manual_search";
    private static final String KEY_INPUTS = "inputs";
    private static final String KEY_EXTRAS = "extras";

    public Boolean getExecute(String key) {
        return getPropertyWithFallback(key, KEY_EXECUTE);
    }

    public boolean isForceManualSearch(String key) {
        return getPropertyWithFallback(key, KEY_FORCE_MANUAL_SEARCH);
    }

    public void setExecute(String key, Boolean value) {
        setProperty(key, value, KEY_EXECUTE);
    }

    public void setForceManualSearch(String key, Boolean value) {
        setProperty(key, value, KEY_FORCE_MANUAL_SEARCH);
    }

    public Hashtable<String, String> getInputs(String key) {
        Map<String, Object> value = (Map<String, Object>) this.get(key);
        if (value != null) {
            Map<String, String> valueInputs = (Map<String, String>) value.get(this.KEY_INPUTS);
            if (valueInputs != null) {
                Hashtable<String, String> inputs = new Hashtable<String, String>();
                for (String inputKey : valueInputs.keySet()) {
                    inputs.put(inputKey, (String) valueInputs.get(inputKey));
                }
                return inputs;
            }
        }
        return null;
    }

    public void setInputs(String key, Hashtable<String, String> value) {
        this.initKey(key);
        ((Map<String, Object>) this.get(key)).put(this.KEY_INPUTS, value);
    }

    public void setExtras(String key, Multimap<String, String> value) {
        this.initKey(key);
        ((Map<String, Object>) this.get(key)).put(this.KEY_EXTRAS, value);
    }

    public Multimap<String, String> getExtras(String key) {
        Map<String, Object> value = (Map<String, Object>) this.get(key);
        if (value != null && value.containsKey(KEY_EXTRAS)) {
            return  (Multimap<String, String>) value.get(KEY_EXTRAS);
        }
        return ImmutableMultimap.of();
    }

    public Boolean hasProperty(String key, String property) {
        Map<String, Object> value = (Map<String, Object>) this.get(key);
        if (value != null) {
            return value.containsKey(property);
        }
        return Boolean.FALSE;
    }

    private void initKey(String key) {
        if (this.get(key) == null) {
            this.put(key, new Hashtable<String, Object>());
        }
    }

    private Boolean getProperty(String key, String property) {
        Map<String, Object> value = (Map<String, Object>) this.get(key);
        if (value != null) {
            return value.containsKey(property) && (Boolean) value.get(property);
        }
        return Boolean.FALSE;
    }

    // temporary method for backward compatibility, will be replaced with original getProperty method
    // without fallback in the subsequent deploy
    private Boolean getPropertyWithFallback(String key, String property) {
        Map<String, Object> value = (Map<String, Object>) this.get(key);
        if (value != null) {
            return value.containsKey(property) && (Boolean) value.get(property);
        }
        return Boolean.FALSE;
    }

    private void setProperty(String key, Boolean value, String property) {
        this.initKey(key);
        ((Map<String, Object>) this.get(key)).put(property, value);
    }

}
