package eagea.nodeio.model.logic.player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Observable;

import eagea.nodeio.model.logic.map.ZoneM;

/**
 * Manage all the game players.
 */
public class PlayersM extends Observable implements Serializable
{
    // Event.
    public enum Event { ADD, REMOVE }

    // Current players on the map.
    private final ArrayList<PlayerM> mPlayers;

    public PlayersM()
    {
        mPlayers = new ArrayList<>();
    }

    public ArrayList<PlayerM> getPlayers()
    {
        return mPlayers;
    }

    public void remove(PlayerM player)
    {
        mPlayers.remove(player);
        // Notify the associated view.
        notify(Event.REMOVE);
    }

    public void add(PlayerM player)
    {
        mPlayers.add(player);
        // Notify the associated view.
        notify(Event.ADD);
    }

    public PlayerM get(int id)
    {
        return mPlayers.get(id);
    }

    public void notify(Event event)
    {
        setChanged();
        notifyObservers(event);
    }

    public int getNbPlayers()
    {
        return mPlayers.size();
    }
}