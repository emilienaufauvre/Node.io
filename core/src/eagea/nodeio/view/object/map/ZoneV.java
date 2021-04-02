package eagea.nodeio.view.object.map;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.Observable;
import java.util.Observer;

import eagea.nodeio.GameScreen;
import eagea.nodeio.model.logic.map.ZoneM;

/**
 * A zone of the game map.
 */
public class ZoneV implements Observer
{
    // Model.
    private final ZoneM mZone;
    // Zone's cells.
    private final CellV[][] mCells;

    public ZoneV(ZoneM zone)
    {
        // Get the model and observe it.
        mZone = zone;
        mZone.addObserver(this);
        // Load the cells.
        mCells = new CellV[ZoneM.SIZE][ZoneM.SIZE];

        for (int i = 0; i < ZoneM.SIZE; i ++)
        {
            for (int j = 0; j < ZoneM.SIZE; j ++)
            {
                mCells[i][j] = new CellV(i, j, getTexture());
            }
        }
    }

    public void render()
    {
        for (int i = ZoneM.SIZE - 1; i >= 0 ; i --)
        {
            for (int j = ZoneM.SIZE - 1; j >= 0 ; j --)
            {
                mCells[i][j].render();
            }
        }
    }

    public void update(float delta)
    {
        for (int i = 0; i < ZoneM.SIZE; i ++)
        {
            for (int j = 0; j < ZoneM.SIZE; j ++)
            {
                mCells[i][j].update(delta);
            }
        }
    }

    @Override
    public void update(Observable observable, java.lang.Object o)
    {
        // Zone model has changed.
    }

    /**
     * Return a random texture for a cell of this zone.
     */
    private TextureRegion getTexture()
    {
        String name = mZone.getType().name().toLowerCase();

        return GameScreen.mMapAtlas.findRegion(name, (int) (Math.random() * 2d) + 1);
    }
}