package org.commcare.formplayer.services;

import io.sentry.SentryLevel;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.util.FormplayerSentry;
import org.javarosa.core.model.utils.TimezoneProvider;

import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by amstone326 on 1/8/18.
 */
public class BrowserValuesProvider extends TimezoneProvider {

    private int timezoneOffsetMillis = -1;
    private TimeZone timezoneFromBrowser = null;

    public void setTimezoneOffset(AuthenticatedRequestBean bean) {
        timezoneOffsetMillis = bean.getTzOffset();
        String tzFromBrowserString = bean.getTzFromBrowser();

        if (tzFromBrowserString != null &&
                Arrays.asList(TimeZone.getAvailableIDs()).contains(tzFromBrowserString)) {
            timezoneFromBrowser = TimeZone.getTimeZone(tzFromBrowserString);
        }

        try {
            checkTzDiscrepancy(bean, timezoneFromBrowser, new Date());
        } catch (TzDiscrepancyException e) {
            FormplayerSentry.captureException(e, SentryLevel.WARNING);
        }
    }

    public void checkTzDiscrepancy(AuthenticatedRequestBean bean,
                                   TimeZone tz,
                                   Date date) throws TzDiscrepancyException {
        int reportedTzOffsetMillis = bean.getTzOffset();
        String reportedTzId = bean.getTzFromBrowser();

        if (reportedTzId == null && reportedTzOffsetMillis == -1) {
            return;
        }
        int tzOffsetFromTz = 0;
        if (tz != null) {
            date = (date == null) ? new Date() : date;
            tzOffsetFromTz = tz.getOffset(date.getTime());
            if (tzOffsetFromTz == reportedTzOffsetMillis) {
                return;
            }
        }
        String tzName = (tz == null) ? null : tz.getDisplayName();
        String errorMsg = String.format("Reported timezone %s generated tz name %s with offset %d which is different " +
                "than reported offset %d", reportedTzId, tzName, tzOffsetFromTz, reportedTzOffsetMillis);
        throw new TzDiscrepancyException(errorMsg);
    }

    @Override
    public int getTimezoneOffsetMillis() {
        return timezoneOffsetMillis;
    }

    @Override
    public TimeZone getTimezone() {
        return timezoneFromBrowser;
    }

    public class TzDiscrepancyException extends Exception {
        public TzDiscrepancyException(String message) {
            super(message);
        }
    }

}
