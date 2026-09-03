package com.freshfish.mathmaster.init;

import com.freshfish.mathmaster.MathMaster;
import com.freshfish.mathmaster.menu.BookshelfQuizMenu;
import com.freshfish.mathmaster.menu.MathMasterGuideMenu;
import com.freshfish.mathmaster.menu.InsightQuizMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MathMaster.MODID);

    public static final Supplier<MenuType<BookshelfQuizMenu>> BOOKSHELF_QUIZ =
            MENUS.register("bookshelf_quiz", () -> IMenuTypeExtension.create(BookshelfQuizMenu::new));

    public static final Supplier<MenuType<MathMasterGuideMenu>> MATHMASTER_GUIDE =
            MENUS.register("mathmaster_guide", () -> IMenuTypeExtension.create(MathMasterGuideMenu::new));

    public static final Supplier<MenuType<InsightQuizMenu>> INSIGHT_QUIZ =
            MENUS.register("insight_quiz", () -> IMenuTypeExtension.create(InsightQuizMenu::new));
}
