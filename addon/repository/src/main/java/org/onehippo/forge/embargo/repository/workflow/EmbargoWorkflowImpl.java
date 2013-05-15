package org.onehippo.forge.embargo.repository.workflow;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jdo.annotations.DatastoreIdentity;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.onehippo.forge.embargo.repository.EmbargoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@PersistenceCapable(identityType = IdentityType.DATASTORE, cacheable = "true", detachable = "false", table = "documents")
@DatastoreIdentity(strategy = IdGeneratorStrategy.NATIVE)
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class EmbargoWorkflowImpl extends WorkflowImpl implements EmbargoWorkflow {

    private static Logger log = LoggerFactory.getLogger(EmbargoWorkflowImpl.class);

    @Persistent(column = "jcr:uuid")
    protected String uuid;

    /**
     * @throws java.rmi.RemoteException
     */
    public EmbargoWorkflowImpl() throws RemoteException {
    }

    @Override
    public void addEmbargo() throws WorkflowException, RepositoryException, MappingException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();

        final Node handle = internalWorkflowSession.getNodeByIdentifier(uuid).getParent();
        if (!handle.isCheckedOut()) {
            internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
        }
        handle.addMixin(EmbargoConstants.EMBARGO_MIXIN_NAME);
        handle.setProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME, getUserGroups(workflowContext.getUserIdentity()));

        internalWorkflowSession.save();
    }

    protected String[] getUserGroups(String userIdentity) throws RepositoryException {
        Query selectGroupsQuery = getWorkflowContext().getInternalWorkflowSession().getWorkspace().getQueryManager().createQuery(
                EmbargoConstants.SELECT_GROUPS_QUERY.replace("{}", userIdentity),
                Query.SQL);
        NodeIterator groupNodes = selectGroupsQuery.execute().getNodes();
        if (!groupNodes.hasNext()) {
            throw new RepositoryException("User does not have the permissions to set/remove embargo");
        }

        List<String> groupNames = new ArrayList<String>();
        List<String> embargoEnabledGroupNames = getAllEmbargoEnabledGroups();

        while (groupNodes.hasNext()) {
            String groupName = groupNodes.nextNode().getName();
            if (embargoEnabledGroupNames.contains(groupName)) {
                groupNames.add(groupName);
            }
        }

        return groupNames.toArray(new String[groupNames.size()]);
    }

    protected List<String> getAllEmbargoEnabledGroups() {
        try {
            Value[] embargoGroups = getWorkflowContext().getInternalWorkflowSession().getRootNode()
                    .getNode(EmbargoConstants.EMBARGO_GROUPS_MAPPING_NODE_PATH).getProperty(HippoNodeType.HIPPO_GROUPS).getValues();
            List<String> embargoGroupNames = new ArrayList<String>();
            for (final Value embargoGroup : embargoGroups) {
                embargoGroupNames.add(embargoGroup.getString());
            }
            return embargoGroupNames;

        } catch (RepositoryException e) {
            log.error("Error while reading list of embargo enabled groups", e);
        }

        return Collections.emptyList();
    }

    @Override
    public void removeEmbargo() throws WorkflowException, RepositoryException, MappingException, RemoteException {
        final WorkflowContext workflowContext = getWorkflowContext();
        final Session internalWorkflowSession = workflowContext.getInternalWorkflowSession();

        final Node handle = internalWorkflowSession.getNodeByIdentifier(uuid).getParent();
        if (!handle.isCheckedOut()) {
            internalWorkflowSession.getWorkspace().getVersionManager().checkout(handle.getPath());
        }

        if (handle.hasProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME)) {
            handle.getProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME).remove();
        }
        handle.removeMixin(EmbargoConstants.EMBARGO_MIXIN_NAME);

        internalWorkflowSession.save();
    }

    @Override
    public void removeEmbargo(final Calendar publicationDate) throws WorkflowException, RepositoryException, MappingException, RemoteException {
        WorkflowContext wfCtx = getWorkflowContext();
        wfCtx = wfCtx.getWorkflowContext(publicationDate);

        EmbargoWorkflow wf = (EmbargoWorkflow) wfCtx.getWorkflow("embargo");
        wf.removeEmbargo();
    }
}
