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

import jakarta.persistence.EntityManager;

import java.lang.reflect.Parameter;

import org.apache.commons.logging.Log;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.aot.generate.AotRepositoryConstructorBuilder;
import org.springframework.data.repository.aot.generate.AotRepositoryMethodBuilder;
import org.springframework.data.repository.aot.generate.RepositoryContributor;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.javapoet.TypeName;

/**
 * @author Christoph Strobl
 */
public class JpaRepsoitoryContributor extends RepositoryContributor {

	public JpaRepsoitoryContributor(AotRepositoryContext repositoryContext) {
		super(repositoryContext);
	}

	@Override
	protected void customizeConstructor(AotRepositoryConstructorBuilder constructorBuilder) {
		constructorBuilder.addParameter("entityManager", TypeName.get(EntityManager.class));
	}

	@Override
	protected void customizeDerivedMethod(AotRepositoryMethodBuilder methodBuilder) {

		methodBuilder.customize((repositoryInformation, metadata, builder) -> {

			Query query = AnnotatedElementUtils.findMergedAnnotation(metadata.getRepositoryMethod(), Query.class);
			if (query != null) {

				builder.beginControlFlow("if($L.isDebugEnabled())", metadata.fieldNameOf(Log.class));
				builder.addStatement("$L.debug(\"invoking generated [$L] method\")", metadata.fieldNameOf(Log.class),
						metadata.getRepositoryMethod().getName());
				builder.endControlFlow();

				builder.addStatement("$T query = this.$L.createQuery($S)", jakarta.persistence.Query.class,
						metadata.fieldNameOf(EntityManager.class), query.value());
				int i = 1;
				for (Parameter parameter : metadata.getRepositoryMethod().getParameters()) {
					builder.addStatement("query.setParameter(" + i + ", " + parameter.getName() + ")");
					i++;
				}
				if (!metadata.returnsVoid()) {
					builder.addStatement("return ($T) query.getResultList()", metadata.getReturnType());
				}
			}
		});
	}
}
