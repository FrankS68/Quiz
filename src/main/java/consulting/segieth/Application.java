package consulting.segieth;

import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.spring.annotation.EnableVaadin;

@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET) // Use Aura.STYLESHEET to use Aura instead
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css") // Your custom styles
@ComponentScan(basePackages = {
	    "consulting.segieth",          // Dein Hauptpaket
	    "de.witchcafe"   // Das Paket des externen Services
	})
@EnableVaadin({"consulting.segieth", "de.witchcafe"})
@EnableJpaRepositories({"consulting.segieth", "de.witchcafe"}) // Findet das Repository-Interface
@EntityScan({"consulting.segieth", "de.witchcafe"}) // Findet die @Entity Klassen
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
