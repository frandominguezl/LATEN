/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ACLMessageTools;

import static Logger.Logger.trimString;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author lcv
 */
public class ACLMessageTools {

    public static final String _NULLVAL = "";

    public static JsonObject getJsonContentACLM(ACLMessage m) {
        JsonObject res = new JsonObject();
        if (isJsonACLM(m)) {
            res = Json.parse(m.getContent()).asObject();
        }
        return res;
    }

    public static boolean isJsonString(String s) {
        boolean res = false;

        if (s == null) {
            return false;
        }
        try {
            res = s.length() > 0 && s.charAt(0) == '{' && Json.parse(s) != null;
        } catch (Exception ex) {
            System.err.println("isJsonString() " + ex.toString() + " " + s);
        }
        return res;

    }

    public static boolean isJsonACLM(ACLMessage m) {
        boolean res = false;

        if (m == null || m.getContent() == null) {
            return false;
        }
        return isJsonString(m.getContent());
    }

    public static JsonObject toJsonACLM(ACLMessage msg) {
        JsonObject res = null;
        String receivers = "";

//        ArrayList <AID> receivers = msg.getAllReceiver()
        for (Iterator iterator = msg.getAllReceiver();
                iterator.hasNext();) {
            AID r = (AID) iterator.next();
            receivers += r.getLocalName()+" ";
        }
        res = new JsonObject().add(
                "performative", ACLMessage.getPerformative(msg.getPerformative())).add(
                "sender", (msg.getSender() == null ? _NULLVAL : msg.getSender().getLocalName())).add(
                "receiver", (!msg.getAllReceiver().hasNext() ? _NULLVAL : receivers)).add(
                "protocol", (msg.getProtocol() == null ? _NULLVAL : msg.getProtocol())).add(
                "encoding", (msg.getEncoding() == null ? _NULLVAL : msg.getEncoding())).add(
                //                "encoding", (msg.getEncoding() == null ? _NULLVAL : msg.getEncoding().substring(0, 5)+"...")).add(
                "conversation", (msg.getConversationId() == null ? _NULLVAL : msg.getConversationId())).add(
                "in-reply-to", (msg.getInReplyTo() == null ? _NULLVAL : msg.getInReplyTo())).add(
                "reply-with", (msg.getReplyWith() == null ? _NULLVAL : msg.getReplyWith()));
        if (isJsonACLM(msg)) {
            res.add("content", (msg.getContent() == null ? new JsonObject() : Json.parse(msg.getContent()).asObject()));
        } else {
            res.add("content", msg.getContent());
        }
        return res;
    }

    public static String fancyWriteACLM(ACLMessage msg) {
        String res = "";
        res += (msg.getSender() == null ? _NULLVAL : "|PFM:" + ACLMessage.getPerformative(msg.getPerformative()))
                + (msg.getSender() == null ? _NULLVAL : "|SND:" + msg.getSender().getLocalName())
                + (msg.getAllReceiver().hasNext() ? "|RCV:" + ((AID) msg.getAllReceiver().next()).getLocalName() : _NULLVAL)
                + (msg.getProtocol() == null ? _NULLVAL : "|PRT:" + msg.getProtocol())
                + (msg.getConversationId() == null ? _NULLVAL : "|CNV" + msg.getConversationId())
                + (msg.getInReplyTo() == null ? _NULLVAL : "|IRT:" + msg.getInReplyTo())
                + (msg.getReplyWith() == null ? _NULLVAL : "|RPW:" + msg.getReplyWith())
                + //                "|CNT:"+msg.getContent()+"|";
                //                "|CNT:"+trimString(msg.getContent(), 20)+"|";
                "|CNT:" + (isJsonACLM(msg) ? trimString(msg.getContent(), 30) : msg.getContent()) + "|";
        return res;
    }

    public static String getDetailsLARVA(ACLMessage msg) {
        String res = "";
        if (msg != null && msg.getContent() != null) {
            if (isJsonACLM(msg)) {
                res = getJsonContentACLM(msg).getString("details", msg.getContent());
            } else {
                res = msg.getContent();
            }
        }
        return res;
    }

    public static ACLMessage respondTo(ACLMessage incoming) {
        ACLMessage outgoing = new ACLMessage();
        outgoing.addReceiver(incoming.getSender());
        outgoing.setSender(((AID) incoming.getAllReceiver().next()));
        outgoing.setContent("");
        if (incoming.getConversationId() != null) {
            outgoing.setConversationId(incoming.getConversationId());
        }
        if (incoming.getReplyWith() != null) {
            outgoing.setInReplyTo(incoming.getReplyWith());
        }
        if (incoming.getProtocol() != null) {
            outgoing.setProtocol(incoming.getProtocol());
        }
        return outgoing;
    }

    public static boolean isAnswerTo(ACLMessage incoming, ACLMessage sent) {
        try {
            return incoming.getConversationId().equals(sent.getConversationId())
                    && incoming.getProtocol().equals(sent.getProtocol())
                    && incoming.getInReplyTo().equals(sent.getReplyWith());
        } catch (Exception Ex) {
            return false;
        }
    }

    public static boolean isBadRecipient(ACLMessage msg) {
        String c = msg.getContent().toUpperCase();
        return (c.contains("MTS-ERROR") && c.contains("FAILED TO FIND AGENT"));
    }

    public static ACLMessage secureACLM(ACLMessage incoming) {
        incoming.setContent((incoming.getContent() == null ? _NULLVAL : incoming.getContent()));
        incoming.setEncoding((incoming.getEncoding() == null ? _NULLVAL : incoming.getEncoding()));
        incoming.setConversationId((incoming.getConversationId() == null ? _NULLVAL : incoming.getConversationId()));
        incoming.setOntology((incoming.getOntology() == null ? _NULLVAL : incoming.getOntology()));
        incoming.setReplyWith((incoming.getReplyWith() == null ? _NULLVAL : incoming.getReplyWith()));
        incoming.setInReplyTo((incoming.getInReplyTo() == null ? _NULLVAL : incoming.getInReplyTo()));
        incoming.setProtocol((incoming.getProtocol() == null ? _NULLVAL : incoming.getProtocol()));
        return incoming;
    }
}
