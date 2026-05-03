package net.enelson.sopbattlepass.reward;

public final class RewardClaimResult {

    private final boolean success;
    private final String message;

    public RewardClaimResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
