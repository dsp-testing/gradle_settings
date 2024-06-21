package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

/**
 * Model for <validation> node in {@link QueryPrompt}
 */
public class QueryPromptCondition implements Externalizable {

    private XPathExpression test;

    @Nullable
    private Text message;

    @SuppressWarnings("unused")
    public QueryPromptCondition() {
    }

    public QueryPromptCondition(XPathExpression test, Text message) {
        this.test = test;
        this.message = message;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        test = (XPathExpression)ExtUtil.read(in, new ExtWrapTagged(), pf);
        message = (Text)ExtUtil.read(in, new ExtWrapNullable(Text.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapTagged(test));
        ExtUtil.write(out, new ExtWrapNullable(message));
    }

    public XPathExpression getTest() {
        return test;
    }

    @Nullable
    public Text getMessage() {
        return message;
    }
}
