package org.javarosa.core.util.externalizable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

public class ExtWrapMultiMap extends ExternalizableWrapper {

    private ExternalizableWrapper keyType;

    /* Constructors for serialization */

    public ExtWrapMultiMap(Multimap val) {
        this(val, null);
    }

    public ExtWrapMultiMap(Multimap val, ExternalizableWrapper keyType) {
        if (val == null) {
            throw new NullPointerException();
        }

        this.val = val;
        this.keyType = keyType;
    }

    /* Constructors for deserialization */

    public ExtWrapMultiMap() {
    }

    public ExtWrapMultiMap(Class keyType) {
        this(new ExtWrapBase(keyType));
    }

    public ExtWrapMultiMap(ExternalizableWrapper keyType) {
        if (keyType == null) {
            throw new NullPointerException();
        }

        this.keyType = keyType;
    }

    @Override
    public ExternalizableWrapper clone(Object val) {
        return new ExtWrapMultiMap((Multimap)val, keyType);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        long size = ExtUtil.readNumeric(in);
        Multimap<Object, Object> multimap = ArrayListMultimap.create();
        for (int i = 0; i < size; i++) {
            Object key = ExtUtil.read(in, keyType, pf);
            long numberOfValues = ExtUtil.readNumeric(in);
            for (long l = 0; l < numberOfValues; l++) {
                multimap.put(key, ExtUtil.read(in, new ExtWrapTagged(), pf));
            }
        }
        val = multimap;
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        Multimap multimap = (Multimap)val;
        ExtUtil.writeNumeric(out, multimap.keySet().size());
        for (Object key : multimap.keySet()) {
            ExtUtil.write(out, keyType == null ? key : keyType.clone(key));
            Collection values = multimap.get(key);
            ExtUtil.writeNumeric(out, values.size());
            Iterator valueIterator = values.iterator();
            while (valueIterator.hasNext()) {
                ExtUtil.write(out, new ExtWrapTagged(valueIterator.next()));
            }
        }
    }

    @Override
    public void metaReadExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        keyType = ExtWrapTagged.readTag(in, pf);
    }

    @Override
    public void metaWriteExternal(DataOutputStream out) throws IOException {
        Multimap multimap = (Multimap)val;
        Object keyTagObj = (keyType == null ? (multimap.size() == 0 ? new Object() : multimap.keys().iterator().next()) : keyType);
        ExtWrapTagged.writeTag(out, keyTagObj);
    }
}
