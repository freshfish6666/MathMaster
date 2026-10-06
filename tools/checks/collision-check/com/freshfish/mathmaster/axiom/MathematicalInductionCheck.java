package com.freshfish.mathmaster.axiom;

/** Focused rules checks for Mathematical Induction's effective-level floor. */
public final class MathematicalInductionCheck {
    private MathematicalInductionCheck() {
    }

    public static int run() {
        if (AxiomDefinition.MATHEMATICAL_INDUCTION.skillType()
                != AxiomDefinition.AxiomSkillType.PASSIVE
                || AxiomDefinition.MATHEMATICAL_INDUCTION.requiredNoteLevel() != 2
                || AxiomDefinition.MATHEMATICAL_INDUCTION.maximumNoteLevel() != 4) {
            throw new AssertionError("Mathematical Induction registration was incorrect");
        }
        if (AxiomEffectManager.applyMathematicalInduction(AxiomDefinition.INVOLUTION, 2, 2) != 3) {
            throw new AssertionError("Level 2 Mathematical Induction did not establish a level 3 floor");
        }
        if (AxiomEffectManager.applyMathematicalInduction(AxiomDefinition.INVOLUTION, 4, 2) != 4) {
            throw new AssertionError("Mathematical Induction downgraded a higher-level axiom");
        }
        if (AxiomEffectManager.applyMathematicalInduction(AxiomDefinition.INVOLUTION, 4, 4) != 5) {
            throw new AssertionError("Level 4 Mathematical Induction did not establish a level 5 floor");
        }
        if (AxiomEffectManager.applyMathematicalInduction(AxiomDefinition.MATHEMATICAL_INDUCTION, 2, 4) != 2) {
            throw new AssertionError("Mathematical Induction boosted itself");
        }
        if (AxiomEffectManager.applyMathematicalInduction(AxiomDefinition.ADDITION_COMMUTATIVITY, 2, 4) != 2) {
            throw new AssertionError("Mathematical Induction boosted a non-Logic axiom");
        }
        if (AxiomEffectManager.applyMathematicalInduction(AxiomDefinition.INVOLUTION, 0, 4) != 0) {
            throw new AssertionError("Mathematical Induction granted an unequipped axiom");
        }
        return 6;
    }
}
