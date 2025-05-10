package com.example.cataniaunited.player;

import com.example.cataniaunited.dto.MessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.websockets.next.WebSocketConnection;
import org.jboss.logging.Logger;

import java.util.Random;
import java.util.UUID;

/** Runtime representation of a connected player. */
public class Player {

    private static final Logger LOG = Logger.getLogger(Player.class);

    private String  username;
    private final String uniqueId;
    private final WebSocketConnection connection;
    private int     victoryPoints = 0;

    /* ------------------------------------------------------------------ */
    /*  Constructors                                                      */
    /* ------------------------------------------------------------------ */

    public Player(WebSocketConnection conn) {
        this("RandomPlayer_" + new Random().nextInt(10_000), conn);
    }

    public Player(String username, WebSocketConnection conn) {
        this.username  = username;
        this.uniqueId  = UUID.randomUUID().toString();
        this.connection = conn;
    }

    /* ------------------------------------------------------------------ */
    /*  Accessors                                                         */
    /* ------------------------------------------------------------------ */

    /** Original method still referenced by PlayerService */
    public String getUniqueId() {                 // ▶ added back
        return uniqueId;
    }

    /** Short alias used in newer code */
    public String getId() {                       // unchanged
        return uniqueId;
    }

    public String  getUsername() { return username; }
    public void    setUsername(String u) { username = u; }
    public int     getVictoryPoints() { return victoryPoints; }
    public void    addVictoryPoints(int v) { victoryPoints += v; }
    public WebSocketConnection getConnection() { return connection; }

    /* ------------------------------------------------------------------ */
    /*  Messaging                                                         */
    /* ------------------------------------------------------------------ */

    /** Send a {@link MessageDTO} to this player without blocking. */
    public void sendMessage(MessageDTO dto) {
        if (connection == null) {
            LOG.warnf("No WS connection for player %s – message dropped!", uniqueId);
            return;
        }
        try {
            String json = new ObjectMapper().writeValueAsString(dto);
            connection.sendText(json)          // non-blocking
                    .subscribe().with(
                            v   -> LOG.debugf("▶ Sent to %s : %s", uniqueId, dto.getType()),
                            err -> LOG.errorf(err, "Failed to send to %s", uniqueId)
                    );
        } catch (Exception e) {
            LOG.errorf(e, "Failed to serialise DTO for player %s", uniqueId);
        }
    }

    @Override public String toString() {
        return "Player{username='%s', id='%s'}".formatted(username, uniqueId);
    }
}
