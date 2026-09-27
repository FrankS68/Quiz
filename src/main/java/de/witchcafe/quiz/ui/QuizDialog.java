package de.witchcafe.quiz.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.nimbusds.jose.shaded.gson.Gson;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;

import consulting.segieth.security.SecurityService;
import de.witchcafe.quiz.Quiz;
import de.witchcafe.quiz.QuizItem;
import de.witchcafe.quiz.QuizItemService;
import de.witchcafe.quiz.QuizService;

public class QuizDialog extends Dialog{

    protected HashMap<QuizItem, String> result = new HashMap<QuizItem, String>();
    protected List<QuizItem> quizItems;
    protected QuizItem quizItem;
    protected QuestionSpan questionSpan;
    protected AnswerSpan answerSpan;
    protected Integer quizIndex = 0;
    protected String category;
    
    public class QuizSpan extends Span{
    	public QuizSpan(String content) {
    		super(content);
    	}
    }
    
    public class QuestionSpan extends QuizSpan{
    	AnswerSpan answerSpan;
    	public QuestionSpan(String content) {
    		super(content);
    	}
    	
    	public AnswerSpan createAnswerSpan(String content) {
    		answerSpan =  new AnswerSpan(this, content);
    		addClickListener(e -> {
		        getStyle().setColor("lightGrey");
		        answerSpan.setVisible(true);
		        answerSpan.setEnabled(true);
	        });
    		return answerSpan;
    	}
    }
    
    public class AnswerSpan extends QuizSpan{
    	QuestionSpan questionSpan;
    	public AnswerSpan(QuestionSpan qs,String content) {
    		super(content);
    		this.questionSpan = qs;
	        setVisible(false);
	        setEnabled(false);
	        addClickListener(e -> {
		        setVisible(false);
		        setEnabled(false);
		        if (++quizIndex >= quizItems.size()) {
		        	createQuizResult();			        	
		        	Notification.show("done", 0, Position.TOP_CENTER, isCloseOnEsc());
		        }
		        else {
			        quizItem = quizItems.get(quizIndex);
			        questionSpan.setText(quizItem.getQuestion());
			        questionSpan.getStyle().setColor("black");
			        answerSpan.setText(quizItem.getAnswer());
		        }
	        });
    	}
    }
    
    private void createQuizResult() {
    	quizService.createQuiz(
    			category,
    			securityService.getUserProfile(),
    			new Gson().toJson(result));	   
        dataProvider.refreshAll();
        Notification.show("Quiz added", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    	close();
    }
    
    protected QuizItemService quizItemService;
    protected QuizService quizService;
    protected SecurityService securityService;
    
    DataProvider<Quiz, ?> dataProvider;
    
    public QuizDialog(QuizListView qiv, String category) {
		this.category = category;
		this.dataProvider = qiv.quizGrid.getDataProvider();
    	this.quizService = qiv.quizService;
    	this.quizItemService = qiv.quizItemService;
    	this.securityService = qiv.securityService;
		quizItems = quizItemService.findByCategory(category);
    	Collections.shuffle(quizItems);
    	quizItems = quizItems.subList(0, 5);
    	
    	quizItem = quizItems.get(quizIndex);
	    Button endButton = new Button("End", e -> {
			quizService.createQuiz(
	    			category,
	    			securityService.getUserProfile(),
	    			new Gson().toJson(result));
			dataProvider.refreshAll();
	        Notification.show("Quiz added", 3000, Notification.Position.BOTTOM_END)
	                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
	    	close();	
	    });
	    Button closeButton = new Button("Close", e -> close());
        getFooter().add(endButton,closeButton);
        getHeader().add(category);
        
        VerticalLayout dialogLayout = buildLayout();
        add(dialogLayout);
    }

    protected VerticalLayout buildLayout() {
		questionSpan = new QuestionSpan(quizItem.getQuestion());
        
        answerSpan = questionSpan.createAnswerSpan(quizItem.getAnswer());
        
        VerticalLayout dialogLayout = new VerticalLayout(questionSpan,answerSpan);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.getStyle().set("width", "22em").set("max-width", "100%");
		return dialogLayout;
	}
}