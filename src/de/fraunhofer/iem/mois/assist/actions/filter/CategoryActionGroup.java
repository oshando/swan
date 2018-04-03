package de.fraunhofer.iem.mois.assist.actions.filter;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import de.fraunhofer.iem.mois.assist.data.JSONFileLoader;
import de.fraunhofer.iem.mois.assist.data.Category;
import de.fraunhofer.iem.mois.assist.util.Constants;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class CategoryActionGroup extends ActionGroup {

    public CategoryActionGroup() {
    }

    public CategoryActionGroup(String name, boolean popup) {
        super(name, popup);
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {

        ArrayList<FilterAction> cweFilter= new ArrayList<FilterAction>();
        ArrayList<FilterAction> typeFilter= new ArrayList<FilterAction>();

        if(JSONFileLoader.isFileSelected()){

            for(Category category: JSONFileLoader.getCategories()){

                if(category.isCwe())
                    cweFilter.add(new FilterAction(new Pair<>(Constants.FILTER_CWE,category.toString())));
                else
                    typeFilter.add(new FilterAction(new Pair<>(Constants.FILTER_TYPE,category.toString())));
            }
        }

        if (this.toString().contains(Constants.FILTER_CWE))
            return cweFilter.toArray(new FilterAction[cweFilter.size()]);
        else
            return typeFilter.toArray(new FilterAction[typeFilter.size()]);
    }

    @Override
    public boolean isPopup() {
        return true;
    }
}
