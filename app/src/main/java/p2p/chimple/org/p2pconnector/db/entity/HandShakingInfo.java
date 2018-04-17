package p2p.chimple.org.p2pconnector.db.entity;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.comparators.ComparatorChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HandShakingInfo {

    @SerializedName("user_id")
    private String userId;

    @SerializedName("device_id")
    private String deviceId;

    @SerializedName("sequence")
    private Long sequence;

    private Long startingSequence;

    public HandShakingInfo() {
    }


    public HandShakingInfo(String userId, String deviceId, Long sequence) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.sequence = sequence;
    }

    public String getUserId() {
        return userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Long getSequence() {
        return sequence;
    }

    public Long getStartingSequence() {
        return startingSequence;
    }

    public void setStartingSequence(Long startingSequence) {
        this.startingSequence = startingSequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        final HandShakingInfo info = (HandShakingInfo) obj;
        if (this == info) {
            return true;
        } else {
            return (this.userId.equals(info.userId) && this.deviceId == info.deviceId && this.sequence == info.sequence);
        }
    }

    @Override
    public int hashCode() {
        int hashno = 7;
        hashno = 13 * hashno + (userId == null ? 0 : userId.hashCode()) + (deviceId == null ? 0 : deviceId.hashCode()) + (sequence == null ? 0 : sequence.hashCode());
        return hashno;
    }
}