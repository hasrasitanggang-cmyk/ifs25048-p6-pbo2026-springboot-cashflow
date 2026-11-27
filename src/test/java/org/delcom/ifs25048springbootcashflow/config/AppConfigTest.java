
package org.delcom.ifs25048springbootcashflow.config;

import org.delcom.ifs25048springbootcashflow.adapter.ai.OpenAiFinanceService;
import org.delcom.ifs25048springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25048springbootcashflow.adapter.repository.JdbcTransactionRepository;
import org.delcom.ifs25048springbootcashflow.adapter.repository.JdbcUserRepository;
import org.delcom.ifs25048springbootcashflow.adapter.repository.SchemaInitializer;
import org.delcom.ifs25048springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25048springbootcashflow.domain.repository.IFinanceAiService;
import org.delcom.ifs25048springbootcashflow.domain.repository.ITransactionRepository;
import org.delcom.ifs25048springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25048springbootcashflow.usecase.AuthUseCase;
import org.delcom.ifs25048springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25048springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25048springbootcashflow.usecase.UserUseCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.mockito.MockedStatic;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;

import javax.sql.DataSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppConfigTest {

    private final AppConfig config = new AppConfig();

    @Test
    void userRepositoryCreatesJdbcRepository() {

        DataSource dataSource = mock(DataSource.class);

        try (MockedStatic<SchemaInitializer> initializer =
                     mockStatic(SchemaInitializer.class)) {

            IUserRepository repository =
                    config.userRepository(dataSource);

            assertInstanceOf(JdbcUserRepository.class, repository);

            initializer.verify(
                    () -> SchemaInitializer.initialize(dataSource)
            );
        }
    }

    @Test
    void transactionRepositoryCreatesJdbcRepository() {

        DataSource dataSource = mock(DataSource.class);

        ITransactionRepository repository =
                config.transactionRepository(dataSource);

        assertInstanceOf(
                JdbcTransactionRepository.class,
                repository
        );
    }

    @Test
    void financeUseCaseCreatesInstance() {

        ITransactionRepository repository =
                mock(ITransactionRepository.class);

        FinanceUseCase useCase =
                config.financeUseCase(repository);

        assertNotNull(useCase);
    }

    @Test
    void financeAiServiceCreatesOpenAiService() {

        IFinanceAiService service =
                config.financeAiService(
                        "https://example.com/api",
                        "test-key",
                        "test-model"
                );

        assertInstanceOf(OpenAiFinanceService.class, service);
    }

    @Test
    void financeAiUseCaseCreatesInstance() {

        IFinanceAiService aiService =
                mock(IFinanceAiService.class);

        ITransactionRepository repository =
                mock(ITransactionRepository.class);

        FinanceAiUseCase useCase =
                config.financeAiUseCase(aiService, repository);

        assertNotNull(useCase);
    }

    @Test
    void authUseCaseCreatesInstance() {

        IUserRepository repository =
                mock(IUserRepository.class);

        AuthUseCase useCase =
                config.authUseCase(repository);

        assertNotNull(useCase);
    }

    @Test
    void userUseCaseCreatesInstance() {

        IUserRepository repository =
                mock(IUserRepository.class);

        UserUseCase useCase =
                config.userUseCase(repository);

        assertNotNull(useCase);
    }

    @Test
    void financePresenterCreatesInstance() {

        FinancePresenter presenter =
                config.financePresenter();

        assertNotNull(presenter);
    }

    @Test
    void fileStorageCreatesInstance(@TempDir Path tempDir) {

        FileStorage storage =
                config.fileStorage(
                        tempDir.resolve("uploads").toString()
                );

        assertNotNull(storage);
    }

    @Test
    void seedRootAdminExecutesRunner() throws Exception {

        AuthUseCase authUseCase =
                mock(AuthUseCase.class);

        ApplicationRunner runner =
                config.seedRootAdmin(
                        authUseCase,
                        "root",
                        "root-password"
                );

        ApplicationArguments arguments =
                mock(ApplicationArguments.class);

        runner.run(arguments);

        verify(authUseCase).ensureRootAdmin(
                "root",
                "root-password"
        );
    }

    @Test
    void uploadResourceConfigurerHandlesDirectory(
            @TempDir Path tempDir
    ) throws Exception {

        Path directory =
                Files.createDirectory(
                        tempDir.resolve("uploads-directory")
                );

        assertTrue(directory.toUri().toString().endsWith("/"));

        verifyUploadLocation(
                directory,
                directory.toUri().toString()
        );
    }

    @Test
    void uploadResourceConfigurerHandlesLocationWithoutSlash(
            @TempDir Path tempDir
    ) throws Exception {

        Path file =
                Files.createFile(
                        tempDir.resolve("uploads-file.tmp")
                );

        assertFalse(file.toUri().toString().endsWith("/"));

        verifyUploadLocation(
                file,
                file.toUri().toString() + "/"
        );
    }

    private void verifyUploadLocation(
            Path root,
            String expectedLocation
    ) {

        FileStorage storage =
                mock(FileStorage.class);

        when(storage.root()).thenReturn(root);

        ResourceHandlerRegistry registry =
                mock(ResourceHandlerRegistry.class);

        ResourceHandlerRegistration registration =
                mock(ResourceHandlerRegistration.class);

        when(registry.addResourceHandler("/uploads/**"))
                .thenReturn(registration);

        config.uploadResourceConfigurer(storage)
                .addResourceHandlers(registry);

        verify(registry)
                .addResourceHandler("/uploads/**");

        verify(registration)
                .addResourceLocations(expectedLocation);
    }
}
