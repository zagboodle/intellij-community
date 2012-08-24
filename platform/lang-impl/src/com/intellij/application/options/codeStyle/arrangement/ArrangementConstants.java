/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.application.options.codeStyle.arrangement;

import org.jetbrains.annotations.NonNls;

/**
 * @author Denis Zhdanov
 * @since 8/13/12 11:48 AM
 */
public class ArrangementConstants {

  @NonNls public static final String ACTION_GROUP_RULE_EDITOR_CONTEXT_MENU = "Arrangement.RuleEditor.Context.Menu";
  @NonNls public static final String RULE_EDITOR_PLACE                     = "Arrangement.RuleEditor.Place";
  @NonNls public static final String RULE_TREE_PLACE                       = "Arrangement.RuleTree.Place";

  public static final boolean LOG_RULE_MODIFICATION = Boolean.parseBoolean(System.getProperty("log.arrangement.rule.modification"));

  private ArrangementConstants() {
  }
}
