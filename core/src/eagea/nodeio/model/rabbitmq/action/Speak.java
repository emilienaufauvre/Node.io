package eagea.nodeio.model.rabbitmq.action;

import eagea.nodeio.model.logic.player.PlayerM;

/**
 * Player wants to talk. Send this action to the host so that it can
 * confirm it, and send it to everyone.
 */
public class Speak extends Action
{
    private static final long serialVersionUID = -9101801388966315170L;

    private final PlayerM.Speak mSentence;

    public Speak(String ID, PlayerM.Speak sentence)
    {
        super(ID);
        mSentence = sentence;
    }

    public PlayerM.Speak getSentence()
    {
        return mSentence;
    }
}