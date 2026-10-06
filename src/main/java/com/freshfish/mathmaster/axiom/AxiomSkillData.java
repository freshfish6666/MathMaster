package com.freshfish.mathmaster.axiom;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class AxiomSkillData implements INBTSerializable<CompoundTag> {
    private int additionCommutativityCooldownTicks;
    private int additiveInverseCooldownTicks;
    private int euclidPrimeInfinityCooldownTicks;
    private int playfairCooldownTicks;
    private int involutionCooldownTicks;
    private int geodesicCooldownTicks;
    private int returnCooldownTicks;
    private int primeComboCooldownTicks;

    public int getPrimeComboCooldownTicks() { return this.primeComboCooldownTicks; }
    public void setPrimeComboCooldownTicks(int ticks) { this.primeComboCooldownTicks = Math.max(0,ticks); }
    private final ReturnAnchorData returnAnchors = new ReturnAnchorData();

    public ReturnAnchorData returnAnchors() { return this.returnAnchors; }
    public int getReturnCooldownTicks() { return this.returnCooldownTicks; }
    public void setReturnCooldownTicks(int ticks) { this.returnCooldownTicks = Math.max(0, ticks); }

    public int getAdditionCommutativityCooldownTicks() {
        return this.additionCommutativityCooldownTicks;
    }

    public void setAdditionCommutativityCooldownTicks(int ticks) {
        this.additionCommutativityCooldownTicks = Math.max(0, ticks);
    }

    public int getAdditiveInverseCooldownTicks() {
        return this.additiveInverseCooldownTicks;
    }

    public void setAdditiveInverseCooldownTicks(int ticks) {
        this.additiveInverseCooldownTicks = Math.max(0, ticks);
    }

    public int getEuclidPrimeInfinityCooldownTicks() {
        return this.euclidPrimeInfinityCooldownTicks;
    }

    public void setEuclidPrimeInfinityCooldownTicks(int ticks) {
        this.euclidPrimeInfinityCooldownTicks = Math.max(0, ticks);
    }

    public int getPlayfairCooldownTicks() {
        return this.playfairCooldownTicks;
    }

    public void setPlayfairCooldownTicks(int ticks) {
        this.playfairCooldownTicks = Math.max(0, ticks);
    }

    public int getInvolutionCooldownTicks() {
        return this.involutionCooldownTicks;
    }

    public void setInvolutionCooldownTicks(int ticks) {
        this.involutionCooldownTicks = Math.max(0, ticks);
    }

    public boolean hasCooldowns() {
        return this.additionCommutativityCooldownTicks > 0
                || this.additiveInverseCooldownTicks > 0
                || this.euclidPrimeInfinityCooldownTicks > 0
                || this.playfairCooldownTicks > 0
                || this.involutionCooldownTicks > 0
                || this.geodesicCooldownTicks > 0
                || this.returnCooldownTicks > 0
                || this.primeComboCooldownTicks > 0;
    }

    public void tickCooldowns() {
        if (this.primeComboCooldownTicks > 0) this.primeComboCooldownTicks--;
        if (this.returnCooldownTicks > 0) this.returnCooldownTicks--;
        if (this.geodesicCooldownTicks > 0) {
            this.geodesicCooldownTicks--;
        }
        if (this.additionCommutativityCooldownTicks > 0) {
            this.additionCommutativityCooldownTicks--;
        }
        if (this.additiveInverseCooldownTicks > 0) {
            this.additiveInverseCooldownTicks--;
        }
        if (this.euclidPrimeInfinityCooldownTicks > 0) {
            this.euclidPrimeInfinityCooldownTicks--;
        }
        if (this.playfairCooldownTicks > 0) {
            this.playfairCooldownTicks--;
        }
        if (this.involutionCooldownTicks > 0) {
            this.involutionCooldownTicks--;
        }
    }

    public void clearCooldowns() {
        this.primeComboCooldownTicks = 0;
        this.returnCooldownTicks = 0;
        this.geodesicCooldownTicks = 0;
        this.additionCommutativityCooldownTicks = 0;
        this.additiveInverseCooldownTicks = 0;
        this.euclidPrimeInfinityCooldownTicks = 0;
        this.playfairCooldownTicks = 0;
        this.involutionCooldownTicks = 0;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("prime_combo_cooldown", this.primeComboCooldownTicks);
        tag.putInt("addition_commutativity_cooldown", this.additionCommutativityCooldownTicks);
        tag.putInt("additive_inverse_cooldown", this.additiveInverseCooldownTicks);
        tag.putInt("euclid_prime_infinity_cooldown", this.euclidPrimeInfinityCooldownTicks);
        tag.putInt("playfair_cooldown", this.playfairCooldownTicks);
        tag.putInt("involution_cooldown", this.involutionCooldownTicks);
        tag.putInt("geodesic_cooldown", this.geodesicCooldownTicks);
        tag.putInt("return_cooldown", this.returnCooldownTicks);
        tag.put("return_anchors", this.returnAnchors.save());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.primeComboCooldownTicks = Math.max(0,tag.getInt("prime_combo_cooldown"));
        this.returnCooldownTicks = Math.max(0, tag.getInt("return_cooldown"));
        this.returnAnchors.load(tag.getCompound("return_anchors"));
        this.additionCommutativityCooldownTicks = Math.max(
                0,
                tag.getInt("addition_commutativity_cooldown")
        );
        this.additiveInverseCooldownTicks = Math.max(0, tag.getInt("additive_inverse_cooldown"));
        this.euclidPrimeInfinityCooldownTicks = Math.max(
                0,
                tag.getInt("euclid_prime_infinity_cooldown")
        );
        this.playfairCooldownTicks = Math.max(0, tag.getInt("playfair_cooldown"));
        this.involutionCooldownTicks = Math.max(0, tag.getInt("involution_cooldown"));
        this.geodesicCooldownTicks = Math.max(0, tag.getInt("geodesic_cooldown"));
    }

    public int getGeodesicCooldownTicks() {
        return this.geodesicCooldownTicks;
    }

    public void setGeodesicCooldownTicks(int ticks) {
        this.geodesicCooldownTicks = Math.max(0, ticks);
    }
}
