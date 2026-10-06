package com.freshfish.mathmaster.axiom;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class PrimeMarkData implements INBTSerializable<CompoundTag> {
    public static final StreamCodec<RegistryFriendlyByteBuf, PrimeMarkData> STREAM_CODEC =
            StreamCodec.of(PrimeMarkData::writeNetwork, PrimeMarkData::readNetwork);

    private int prime = 2;
    private long expiresAtGameTime;

    public int getPrime() {
        return this.prime;
    }

    public void setPrime(int prime) {
        this.prime = Math.max(2, prime);
    }

    public void setMark(int prime, long expiresAtGameTime) {
        this.setPrime(prime);
        this.expiresAtGameTime = Math.max(0L, expiresAtGameTime);
    }

    public boolean isActive(long gameTime) {
        return this.expiresAtGameTime > gameTime;
    }

    public void clear() {
        this.expiresAtGameTime = 0L;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("prime", this.prime);
        tag.putLong("expires_at", this.expiresAtGameTime);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.setPrime(tag.getInt("prime"));
        this.expiresAtGameTime = Math.max(0L, tag.getLong("expires_at"));
    }

    private static void writeNetwork(RegistryFriendlyByteBuf buffer, PrimeMarkData data) {
        buffer.writeVarInt(data.prime);
        buffer.writeVarLong(data.expiresAtGameTime);
    }

    private static PrimeMarkData readNetwork(RegistryFriendlyByteBuf buffer) {
        PrimeMarkData data = new PrimeMarkData();
        data.setPrime(buffer.readVarInt());
        data.expiresAtGameTime = Math.max(0L, buffer.readVarLong());
        return data;
    }
}
