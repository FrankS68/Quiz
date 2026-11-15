package de.witchcafe.quiz.ui;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.nimbusds.jose.shaded.gson.Gson;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import de.witchcafe.ui.ViewToolbar;
import de.witchcafe.auth.CurrentUser;
import de.witchcafe.auth.domain.AppUser;
import de.witchcafe.quiz.Quiz;
import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizService;
import jakarta.annotation.security.PermitAll;

@Route("/quizzes")
@RouteAlias("") // Startseite (Ziel nach dem witch-auth-Login)
@PageTitle("Quiz List")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Quiz List")
@PermitAll
public class QuizListView extends VerticalLayout {

    private final QuizService quizService;
    private final QuizItemService quizItemService;
    private final CurrentUser currentUser;

    final Button createBtn;
    final Grid<Quiz> quizGrid;

    QuizListView(
    		QuizService quizService,
    		QuizItemService quizItemService,
    		CurrentUser currentUser) {
        this.quizService = quizService;
        this.quizItemService = quizItemService;
        this.currentUser = currentUser;


        MenuBar createMenu = buildQuizMenu(quizItemService);
        createBtn = new Button("Create", event -> startQuiz("some category"));
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        quizGrid = new Grid<>();
        quizGrid.setItems(query -> quizService.list(toSpringPageRequest(query)).stream());
        quizGrid.addColumn(Quiz::getCategory).setHeader("Category");
        quizGrid.addComponentColumn(item -> {
            AppUser user = item.getUser();
            Span userSpan = new Span(user == null ? "Gast" : user.getName());
            if (user != null) {
                userSpan.setTitle(user.getProvider());
            }
            return userSpan;
        }).setHeader("User");

        quizGrid.setEmptyStateText("You have no quiz to review");
        quizGrid.setSizeFull();
        quizGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().setOverflow(Style.Overflow.HIDDEN);

        ViewToolbar viewToolbar = new ViewToolbar("Quiz List", ViewToolbar.group(createBtn,createMenu));
		add(viewToolbar);
        add(quizGrid);
    }
    
    private MenuBar buildQuizMenu(QuizItemService quizItemService) {
		MenuBar quizMenu = new MenuBar();
        MenuItem droneItem = quizMenu.addItem("Dronen");
        SubMenu droneMenu = droneItem.getSubMenu();
        
        quizItemService.lookupCategories().forEach(result -> {
        	MenuItem quizItem = droneMenu.addItem(result[0].toString());
        	quizItem.addClickListener(event -> {
        		startQuiz(result[0].toString());
        	});
        });
		return quizMenu;
	}

    private void startQuiz(String category) {
    	new QuizDialog(category, quizService, quizItemService, currentUser, quizGrid).open();
    }

}
