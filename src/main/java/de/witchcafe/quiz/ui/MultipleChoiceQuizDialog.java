package de.witchcafe.quiz.ui;

import java.util.ArrayList;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class MultipleChoiceQuizDialog extends QuizDialog {

	private OptionsSpan optionsSpan;

	public MultipleChoiceQuizDialog(QuizListView qiv, String category) {
		super(qiv, category);
	}
	
    public class OptionSpan extends QuizSpan{
    	OptionsSpan optionsSpan;
    	public OptionSpan(OptionsSpan os,String content) {
    		super(content);
    		this.optionsSpan = os;
	        setVisible(false);
	        setEnabled(false);
	        addClickListener(e -> {
		        setVisible(false);
		        setEnabled(false);
		        if (++quizIndex >= quizItems.size()) {
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
    
    public class OptionsSpan extends QuizSpan{
    	QuestionSpan questionSpan;
    	public OptionsSpan(QuestionSpan qs,ArrayList<String> options) {
    		super("");
    		this.questionSpan = qs;
	        setVisible(false);
	        setEnabled(false);
	        options.forEach(option -> add(new OptionSpan(this, option)));
	        
	        addClickListener(e -> {
		        setVisible(false);
		        setEnabled(false);
		        if (++quizIndex >= quizItems.size()) {
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
    


	public AnswerSpan createAnswerSpan(QuestionSpan questionSpan, String content) {
		answerSpan =  new AnswerSpan(questionSpan, content);
		questionSpan.addAttachListener(e -> {
	        getStyle().setColor("lightGrey");
	        answerSpan.setVisible(true);
	        answerSpan.setEnabled(true);
        });
		return answerSpan;
	}	

	public OptionsSpan createOptions(QuestionSpan questionSpan, ArrayList<String> options) {
		optionsSpan =  new OptionsSpan(questionSpan,options);
		questionSpan.addAttachListener(e -> {
	        getStyle().setColor("lightGrey");
	        answerSpan.setVisible(true);
	        answerSpan.setEnabled(true);
        });
		return optionsSpan;
	}	
	
    protected VerticalLayout buildLayout() {
		questionSpan = new QuestionSpan(quizItem.getQuestion());
        
		optionsSpan = createOptions(questionSpan,quizItem.getOptions());
		
        answerSpan = createAnswerSpan(questionSpan,quizItem.getAnswer());
        
        VerticalLayout dialogLayout = new VerticalLayout(questionSpan,answerSpan);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.getStyle().set("width", "22em").set("max-width", "100%");
		return dialogLayout;
	}
    
    

}
