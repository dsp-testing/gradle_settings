package org.commcare.formplayer.beans.menus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.commcare.cases.entity.Entity;
import org.commcare.cases.entity.NodeEntityFactory;
import org.commcare.core.graph.model.GraphData;
import org.commcare.core.graph.util.GraphException;
import org.commcare.formplayer.util.FormplayerGraphUtil;
import org.commcare.modern.util.Pair;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.Style;
import org.commcare.util.screen.EntityDetailSubscreen;
import org.commcare.util.screen.EntityScreenContext;
import org.commcare.util.screen.EntityScreenHelper;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import datadog.trace.api.Trace;

/**
 * Represents one detail tab in a case details page.
 */
@JsonIgnoreProperties
public class EntityDetailResponse {
    protected Object[] details;
    private EntityBean[] entities;
    protected Style[] styles;
    protected String[] headers;
    protected String[] altText;
    protected String title;
    protected boolean isUseNodeset;

    private boolean usesCaseTiles;
    private int maxWidth;
    private int maxHeight;
    private int numEntitiesPerRow;
    private Tile[] tiles;
    private boolean useUniformUnits;
    private boolean hasInlineTile;

    public EntityDetailResponse() {
    }

    public EntityDetailResponse(EntityDetailSubscreen entityScreen, String title) {
        this.title = title;
        this.details = processDetails(entityScreen.getData());
        this.headers = entityScreen.getHeaders();
        this.styles = entityScreen.getStyles();
        this.tiles = processCaseTiles(entityScreen.getDetail());
        this.altText = entityScreen.getAltText();
    }

    private static Object[] processDetails(Object[] data) {
        Object[] ret = new Object[data.length];
        for (int i = 0; i < data.length; i++) {
            Object datum = data[i];
            if (datum instanceof GraphData) {
                try {
                    datum = FormplayerGraphUtil.getHtml((GraphData)datum, "").replace("\"", "'");
                } catch (GraphException e) {
                    datum = "Error loading graph " + e;
                }
            }
            ret[i] = datum;
        }
        return ret;
    }

    // Constructor used for persistent case tile
    public EntityDetailResponse(Detail detail, EvaluationContext ec) {
        this(new EntityDetailSubscreen(0, detail, ec, new String[]{}), "Details");
        processCaseTiles(detail);
        processStyles(detail);
    }

    // Constructor used for detail with nodeset
    public EntityDetailResponse(Detail detail,
            Vector<TreeReference> references,
            EvaluationContext ec,
            String title,
            boolean isFuzzySearchEnabled) {
        EntityScreenContext entityScreenContext = new EntityScreenContext(0, null, 0, Integer.MAX_VALUE, null,
                null, isFuzzySearchEnabled);
        TreeReference[] refs = references.toArray(new TreeReference[references.size()]);
        List<Entity<TreeReference>> entityRefs = EntityScreenHelper.initEntities(ec, detail, entityScreenContext,
                refs);
        List<EntityBean> entityList = EntityListResponse.processEntitiesForCaseList(entityRefs, ec, null);
        this.entities = new EntityBean[entityList.size()];
        entityList.toArray(this.entities);
        this.title = title;
        this.styles = processStyles(detail);
        this.tiles = processCaseTiles(detail);
        Pair<String[], int[]> pair = EntityListResponse.processHeader(detail, ec, 0);
        setHeaders(pair.first);
        setUseNodeset(true);
    }

    private Tile[] processCaseTiles(Detail detail) {
        DetailField[] fields = detail.getFields();
        if (!detail.usesEntityTileView()) {
            return null;
        }
        tiles = new Tile[fields.length];
        setUsesCaseTiles(true);
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].isCaseTileField()) {
                tiles[i] = new Tile(fields[i]);
            } else {
                tiles[i] = null;
            }
        }
        numEntitiesPerRow = detail.getNumEntitiesToDisplayPerRow();
        Pair<Integer, Integer> maxWidthHeight = detail.getMaxWidthHeight();
        maxWidth = maxWidthHeight.first;
        maxHeight = maxWidthHeight.second;
        useUniformUnits = detail.useUniformUnitsInCaseTile();
        return tiles;
    }

    protected static Style[] processStyles(Detail detail) {
        DetailField[] fields = detail.getFields();
        Style[] styles = new Style[fields.length];
        int i = 0;
        for (DetailField field : fields) {
            Style style = new Style(field);
            styles[i] = style;
            i++;
        }
        return styles;
    }

    public Object[] getDetails() {
        return details;
    }

    public void setDetails(Object[] data) {
        this.details = data;
    }

    public Style[] getStyles() {
        return styles;
    }

    public void setStyles(Style[] styles) {
        this.styles = styles;
    }

    public String[] getHeaders() {
        return headers;
    }

    public void setHeaders(String[] headers) {
        this.headers = headers;
    }

    @Override
    public String toString() {
        return "EntityDetailResponse [details=" + Arrays.toString(details)
                + ", styles=" + Arrays.toString(styles)
                + ", headers=" + Arrays.toString(headers) + "]";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String[] getAltText() {
        return altText;
    }

    public void setAltText(String[] altText) {
        this.altText = altText;
    }

    public boolean isUsesCaseTiles() {
        return usesCaseTiles;
    }

    public void setUsesCaseTiles(boolean usesCaseTiles) {
        this.usesCaseTiles = usesCaseTiles;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public int getNumEntitiesPerRow() {
        return numEntitiesPerRow;
    }

    public void setNumEntitiesPerRow(int numEntitiesPerRow) {
        this.numEntitiesPerRow = numEntitiesPerRow;
    }

    public Tile[] getTiles() {
        return tiles;
    }

    public void setTiles(Tile[] tiles) {
        this.tiles = tiles;
    }

    public boolean isUseUniformUnits() {
        return useUniformUnits;
    }

    public void setUseUniformUnits(boolean useUniformUnits) {
        this.useUniformUnits = useUniformUnits;
    }

    public boolean isUseNodeset() {
        return isUseNodeset;
    }

    public void setUseNodeset(boolean useNodeset) {
        isUseNodeset = useNodeset;
    }

    public EntityBean[] getEntities() {
        return entities;
    }

    public void setEntities(EntityBean[] entities) {
        this.entities = entities;
    }

    public boolean getHasInlineTile() {
        return hasInlineTile;
    }

    public void setHasInlineTile(boolean hasInlineTile) {
        this.hasInlineTile = hasInlineTile;
    }
}
