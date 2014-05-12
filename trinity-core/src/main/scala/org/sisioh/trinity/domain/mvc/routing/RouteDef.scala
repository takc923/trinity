/*
 * Copyright 2013 Sisioh Project and others. (http://sisioh.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.sisioh.trinity.domain.mvc.routing

import org.sisioh.trinity.domain.io.http.Methods
import org.sisioh.trinity.domain.mvc.action.Action
import org.sisioh.trinity.domain.mvc.http.{Request, Response}
import org.sisioh.trinity.domain.mvc.routing.pathpattern.PathPattern

/**
 * Represents the defined values to route.
 *
 * @param method [[Methods.Value]]
 * @param pathPattern [[PathPattern]]
 * @param action [[Action]]
 */
case class RouteDef
(method: Methods.Value,
 pathPattern: PathPattern,
 action: Action[Request, Response])


