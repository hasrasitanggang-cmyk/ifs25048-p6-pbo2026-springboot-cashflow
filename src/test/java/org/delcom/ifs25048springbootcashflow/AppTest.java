
package org.delcom.ifs25048springbootcashflow;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.*;

class AppTest {

    @Test
    void mainLoadsDotEnvAndStartsSpringApplication()
            throws Exception {

        String[] args = {
            "--spring.main.web-application-type=none"
        };

        try (
            MockedStatic<DotEnv> dotenv =
                mockStatic(DotEnv.class);

            MockedStatic<SpringApplication> spring =
                mockStatic(SpringApplication.class)
        ) {
            App.main(args);

            dotenv.verify(DotEnv::load);

            spring.verify(
                () -> SpringApplication.run(App.class, args)
            );
        }
    }
}
