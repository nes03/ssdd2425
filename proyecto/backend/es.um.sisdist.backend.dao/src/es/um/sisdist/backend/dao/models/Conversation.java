package es.um.sisdist.backend.dao.models;

import java.sql.Timestamp;

public class Conversation {
    // private int id;
    private String dialogueId;
    private String userId;
    private String prompt;
    private String response;
    private Timestamp createdAt;

    private String dname;
    private String status; // 'READY', 'BUSY', 'FINISHED'
    private String dialogue;

    public Conversation() {
    }

    // public int getId() {
    // return id;
    // }

    public String getDialogueId() {
        return dialogueId;
    }

    // public void setId(int id) {
    // this.id = id;
    // }

    public void setDialogueId(String dialogueId) {
        this.dialogueId = dialogueId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getDname() {
        return dname;
    }

    public void setDname(String dname) {
        this.dname = dname;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDialogue() {
        return dialogue;
    }

    public void setDialogue(String dialogue) {
        this.dialogue = dialogue;
    }
}