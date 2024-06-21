package org.commcare.core.interfaces;

import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.model.instance.TreeElement;

/**
 * Fetches remote instance definitions from cache or by making a remote web call
 */
public interface RemoteInstanceFetcher {

    AbstractTreeElement getExternalRoot(String instanceId, ExternalDataInstanceSource source, String refId)
            throws RemoteInstanceException;

    VirtualDataInstanceStorage getVirtualDataInstanceStorage();

    class RemoteInstanceException extends Exception {

        public RemoteInstanceException(String message) {
            super(message);
        }

        public RemoteInstanceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
