package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

/**
 * Defines a polygon region to be displayed on a map
 */
public class GeoOverlay implements Externalizable {

    @Nullable
    private DisplayUnit coordinates;

    @Nullable
    private DisplayUnit label;

    /**
     * Serialization Only
     */
    public GeoOverlay() {
    }

    public GeoOverlay(DisplayUnit label, DisplayUnit coordinates) {
        this.label = label;
        this.coordinates = coordinates;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        coordinates = (DisplayUnit)ExtUtil.read(in, new ExtWrapNullable(DisplayUnit.class), pf);
        label = (DisplayUnit)ExtUtil.read(in, new ExtWrapNullable(DisplayUnit.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapNullable(coordinates));
        ExtUtil.write(out, new ExtWrapNullable(label));
    }

    public DisplayUnit getCoordinates() {
        return coordinates;
    }

    public DisplayUnit getLabel() {
        return label;
    }
}
