/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.jpa.repository.aot.generated;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.core.test.tools.TestCompiler;

import com.example.UserRepository;

/**
 * @author Christoph Strobl
 */
class JpaRepositoryContributorUnitTests {

	@Test
	public void testCompile() {

		TestJpaAotRepsitoryContext aotContext = new TestJpaAotRepsitoryContext(UserRepository.class, null);
		TestGenerationContext generationContext = new TestGenerationContext(UserRepository.class);

		new JpaRepsoitoryContributor(aotContext).contribute(generationContext);
		generationContext.writeGeneratedContent();

		TestCompiler.forSystem().with(generationContext).compile(compiled -> {
			assertThat(compiled.getAllCompiledClasses()).map(Class::getName).contains("com.example.UserRepositoryImpl__Aot");
		});
	}

}
