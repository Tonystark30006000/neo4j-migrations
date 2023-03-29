/*
 * Copyright 2020-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ac.simons.neo4j.migrations.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Mockito;
import org.neo4j.driver.Driver;

/**
 * @author Michael J. Simons
 * @soundtrack Obiymy Doschu - Son
 */
class MigrationsTest {

	@Test
	void logMessageSupplierForCallbacksShouldWorkWithoutDescription() {

		Callback callback = mock(Callback.class);
		when(callback.getOptionalDescription()).thenReturn(Optional.empty());

		Supplier<String> logMessageSupplier = Migrations.logMessageSupplier(callback, LifecyclePhase.BEFORE_CLEAN);
		assertThat(logMessageSupplier.get()).isEqualTo("Invoked beforeClean callback.");
	}

	@Test
	void logMessageSupplierForCallbacksShouldWorkWithDescription() {

		Callback callback = mock(Callback.class);
		when(callback.getOptionalDescription()).thenReturn(Optional.of("Hallo, Welt."));

		Supplier<String> logMessageSupplier = Migrations.logMessageSupplier(callback, LifecyclePhase.BEFORE_CLEAN);
		assertThat(logMessageSupplier.get()).isEqualTo("Invoked \"Hallo, Welt.\" before clean.");
	}

	@Test
	void deletingAVersionShouldRequireAVersion() {

		Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), mock(Driver.class));
		assertThatIllegalArgumentException().isThrownBy(() -> migrations.delete(null)).withMessage("A valid version must be passed to the delete operation");
	}

	@Test // GH-706
	void shouldOptimizeAllMigrations() throws InvocationTargetException, IllegalAccessException {

		var migrations = new Migrations(MigrationsConfig.builder()
			.withLocationsToScan(
				"classpath:my/awesome/migrations/moreStuff"
			)
			.build(), Mockito.mock(Driver.class));

		var getMigrations = ReflectionUtils.findMethod(Migrations.class, "getMigrations", Migrations.MigrationFilter.class)
			.orElseThrow();
		getMigrations.setAccessible(true);

		var all = getMigrations.invoke(migrations, Migrations.MigrationFilter.ALL);
		var forward = getMigrations.invoke(migrations, Migrations.MigrationFilter.FORWARD_ONLY);
		assertThat(all).isSameAs(forward);
		var undo = getMigrations.invoke(migrations, Migrations.MigrationFilter.UNDO_ONLY);
		assertThat(undo).isSameAs(List.of());
	}

	@SuppressWarnings("unchecked")
	@Test // GH-706
	void shouldOptimizeAllMigrationsWithDuplicates() throws InvocationTargetException, IllegalAccessException {

		var migrations = new Migrations(MigrationsConfig.builder()
			.withLocationsToScan(
				"classpath:non-duplicate"
			)
			.build(), Mockito.mock(Driver.class));

		var getMigrations = ReflectionUtils.findMethod(Migrations.class, "getMigrations", Migrations.MigrationFilter.class)
			.orElseThrow();
		getMigrations.setAccessible(true);

		var all = (List<Migrations>) getMigrations.invoke(migrations, Migrations.MigrationFilter.ALL);
		assertThat(all).hasSize(2);
		var forward = (List<Migrations>) getMigrations.invoke(migrations, Migrations.MigrationFilter.FORWARD_ONLY);
		assertThat(forward).hasSize(1);
		var undo = (List<Migrations>) getMigrations.invoke(migrations, Migrations.MigrationFilter.UNDO_ONLY);
		assertThat(undo).hasSize(1);
	}
}
