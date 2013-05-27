/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.embargo.repository.workflow;

import java.rmi.RemoteException;
import java.util.Calendar;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

/**
 * @version $Id$
 */
public interface EmbargoWorkflow extends Workflow{

    public void addEmbargo() throws WorkflowException, RepositoryException, MappingException, RemoteException;

    public void removeEmbargo() throws WorkflowException, RepositoryException, MappingException, RemoteException;

    public void scheduleRemoveEmbargo(Calendar publicationDate) throws WorkflowException, RepositoryException, MappingException, RemoteException;

    public void cancelSchedule() throws WorkflowException, RepositoryException, MappingException, RemoteException;

}
