package eagea.nodeio.model;

import com.badlogic.gdx.math.Vector3;

import eagea.nodeio.GameScreen;
import eagea.nodeio.model.logic.map.MapM;
import eagea.nodeio.model.logic.map.ZoneM;
import eagea.nodeio.model.logic.player.PlayerM;
import eagea.nodeio.model.logic.player.PlayersM;
import eagea.nodeio.model.rabbitmq.Node;
import eagea.nodeio.model.rabbitmq.action.Action;
import eagea.nodeio.model.rabbitmq.action.Connection;
import eagea.nodeio.model.rabbitmq.action.Disconnection;
import eagea.nodeio.model.rabbitmq.action.Speak;
import eagea.nodeio.model.rabbitmq.action.Move;

/**
 * Handle all the logic of the game, and the rabbitMQ communications with other
 * players.
 */
public class Model
{
    // Player ID (i.e. its color, and its zone type).
    public enum Type { BLACK, GRASS, GRAVEL, ROCK, SAND, SNOW }

    // Context.
    private final GameScreen mGameScreen;

    // RabbitMQ.
    private final Node mNode;
    // The map.
    private MapM mMap;
    // The player.
    private PlayerM mPlayer;
    // All the players.
    private PlayersM mPlayers;

    public Model(GameScreen screen)
    {
        mGameScreen = screen;
        mNode = new Node(this);

        askForConnection();
    }

    /**
     * Action.
     * Connect player to the host.
     */
    private void askForConnection()
    {
        if (! mNode.isHost())
        {
            // Not the host; request for game model.
            mNode.notifyHost(new Connection());
        }
        else
        {
            // The host initiates game model:
            // - Create Map.
            mMap = new MapM();
            // - Create player.
            mPlayer = new PlayerM(0, 0, 0, mMap);
            mPlayers = new PlayersM();
            mPlayers.add(mPlayer);
            // - Create player's zone.
            ZoneM zone = new ZoneM(mPlayer,
                    Type.values()[(int) (Math.random() * Type.values().length)], 0);
            mMap.add(zone);
            // Start rendering.
            mGameScreen.onModelIsReady(this);
        }
    }

    /**
     * Action.
     * Ask the host for moving player.
     */
    public void askForMove(PlayerM.Event orientation)
    {
        // Request for move.
        mNode.notifyHost(new Move(mPlayer, orientation));
    }

    /**
     * Action.
     * Ask the host for speaking.
     */
    public void askForSpeak(PlayerM.Speak sentence)
    {
        // Request for move.
        mNode.notifyHost(new Speak(mPlayer, sentence));
    }

    /**
     * Action.
     * Disconnect the player from the host.
     */
    public void askForDisconnection()
    {
    }

    /**
     * Process and play action received from host.
     */
    public void play(Action action)
    {
        System.out.println("[DEBUG]: play " + action.getClass().getSimpleName());

        if (action instanceof Connection)
        {
            playConnection((Connection) action);
        }
        else if (action instanceof Move)
        {
            playMove((Move) action);
        }
        else if (action instanceof Speak)
        {
            playSpeak((Speak) action);
        }
        else if (action instanceof Disconnection)
        {
            playDisconnection((Disconnection) action);
        }
    }

    private void playConnection(Connection action)
    {
        boolean iamTheNewGuy = (mPlayer == null);

        if (iamTheNewGuy)
        {
            // Set map and players.
            mMap = action.getMap();
            mPlayers = action.getPlayers();
            mPlayer = mPlayers.get(mPlayers.getNbPlayers() - 1);
            // Start rendering.
            mGameScreen.onModelIsReady(this);
        }
        else
        {
            if (! mNode.isHost())
            {
                // Update map and players.
                MapM map = action.getMap();
                PlayersM players = action.getPlayers();

                mMap.add(map.get(map.getNbZones() - 1));
                mPlayers.add(players.get(players.getNbPlayers() - 1));
                mMap.notify(MapM.Event.ADD);
            }
        }
    }

    private void playMove(Move action)
    {
        // Find the corresponding player (reference).
        PlayerM player = mPlayers.find(action.getPlayer());
        // Check if found.
        if (player == null)
        {
            System.err.println("[ERROR]: can't play action");
            return;
        }
        // Move it.
        switch (action.getOrientation())
        {
            case LEFT: player.moveLeft(); break;
            case RIGHT: player.moveRight(); break;
            case UP: player.moveUp(); break;
            case DOWN: player.moveDown(); break;
        }
    }

    private void playSpeak(Speak action)
    {
        // Find the corresponding player (reference).
        PlayerM player = mPlayers.find(action.getPlayer());
        // Check if found.
        if (player == null)
        {
            System.err.println("[ERROR]: can't play action");
            return;
        }
        // Make her/him speak.
        player.speak(action.getSentence());
    }

    private void playDisconnection(Disconnection action)
    {
    }

    /**
     * Host only.
     * Check if the action can be done.
     * If so, return the corresponding one, otherwise null.
     */
    public Action check(Action action)
    {
        System.out.println("[DEBUG]: HOST check " + action.getClass().getSimpleName());

        if (action instanceof Connection)
        {
            return checkConnection((Connection) action);
        }
        else if (action instanceof Move)
        {
            return checkMove((Move) action);
        }
        else if (action instanceof Speak)
        {
            return checkSpeak((Speak) action);
        }
        else if (action instanceof Disconnection)
        {
            return checkDisconnection((Disconnection) action);
        }

        return null;
    }

    private Action checkConnection(Connection action)
    {
        // Add new player and zone.
        PlayerM player = new PlayerM(0, 0, mMap.getNbZones(), mMap);
        ZoneM zone = new ZoneM(player,
                Type.values()[(int) (Math.random() * Type.values().length)],
                mMap.getNbZones());
        mPlayers.add(player);
        mMap.add(zone);
        // Send it.
        return new Connection(mMap, mPlayers);
    }

    private Action checkMove(Move action)
    {
        final Action[] result = { action };
        PlayerM player = action.getPlayer();
        Vector3 position = new Vector3(player.getI(), player.getJ(),
                player.getZone());
        // Get the cell in which the player would like to go.
        switch (((Move) action).getOrientation())
        {
            case LEFT: position.y ++; break;
            case RIGHT: position.y --; break;
            case UP: position.x ++; break;
            case DOWN: position.x --; break;
        }
        // Transform position (may change of zone).
        if (position.x >= ZoneM.SIZE)
        {
            position.x -= ZoneM.SIZE;
            position.z += MapM.ZONE_LINE;
        }
        else if (position.x < 0)
        {
            position.x += ZoneM.SIZE;
            position.z -= MapM.ZONE_LINE;
        }
        else if (position.y >= ZoneM.SIZE)
        {
            position.y -= ZoneM.SIZE;
            position.z += 1;
        }
        else if (position.y < 0)
        {
            position.y += ZoneM.SIZE;
            position.z -= 1;
        }
        // Check if a player is already in this cell.
        mPlayers.getPlayers().forEach(p ->
                {

                    if (p.getZone() == position.z
                            && p.getI() == position.x
                            && p.getJ() == position.y)
                    {
                        // Can't do this move.
                        result[0] = null;
                    }
                }
        );
        // Send it if not null.
        return result[0];
    }

    private Action checkSpeak(Speak action)
    {
        // TODO check if connected.
        // Send it.
        return action;
    }

    private Action checkDisconnection(Disconnection action)
    {
        // Send it.
        return null;
    }

    public MapM getMap()
    {
        return mMap;
    }

    public PlayerM getPlayer()
    {
        return mPlayer;
    }

    public PlayersM getPlayers()
    {
        return mPlayers;
    }
}