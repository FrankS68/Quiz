package de.witchcafe.quiz.ui;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import consulting.segieth.base.ui.ViewToolbar;
import consulting.segieth.security.SecurityService;
import de.witchcafe.quiz.Quiz;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizService;
import jakarta.annotation.security.PermitAll;

@Route("/quizzes")
@PageTitle("Quiz List")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Quiz List")
@PermitAll
class QuizListView extends VerticalLayout {

    private final QuizService quizService;
    private final QuizItemService quizItemService;
    private final SecurityService securityService;

    final Button createBtn;
    final Grid<Quiz> quizGrid;

    QuizListView(
    		QuizService quizService,
    		QuizItemService quizItemService,
    		SecurityService securityService) {
        this.quizService = quizService;
        this.quizItemService = quizItemService;
        this.securityService = securityService;


        createBtn = new Button("Create", event -> createQuiz());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        quizGrid = new Grid<>();
        quizGrid.setItems(query -> quizService.list(toSpringPageRequest(query)).stream());
        quizGrid.addColumn(Quiz::getCategory).setHeader("Category");
        quizGrid.addColumn(Quiz::getUser).setHeader("User");
        quizGrid.addComponentColumn(item -> {
            Span userSpan = new Span(item.getUser().getName());
            userSpan.setTitle(item.getUser().getProvider());
            return userSpan;
        }).setHeader("Aktionen");

        quizGrid.setEmptyStateText("You have no quiz to review");
        quizGrid.setSizeFull();
        quizGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().setOverflow(Style.Overflow.HIDDEN);

        add(new ViewToolbar("Quiz List", ViewToolbar.group(createBtn)));
        add(quizGrid);
    }

    private void createQuiz() {
    	quizItemService.lookupCategories().forEach(resultSet -> System.out.println(resultSet[0]+":"+resultSet[1]));
    	quizService.createQuiz("some category",securityService.getUserProfile());
        quizGrid.getDataProvider().refreshAll();
        Notification.show("Quiz added", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

}
