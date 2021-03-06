/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.logging;

import java.util.Comparator;
import java.util.logging.Handler;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.PathInfoHandler;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.logging.logmanager.PropertySorter.DefaultPropertySorter;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class AbstractFileHandlerDefinition extends AbstractHandlerDefinition {

    public static final String CHANGE_FILE_OPERATION_NAME = "change-file";

    private final ResolvePathHandler resolvePathHandler;
    private final PathInfoHandler diskUsagePathHandler;
    private final boolean registerLegacyOps;

    protected AbstractFileHandlerDefinition(final PathElement path, final Class<? extends Handler> type,
                                            final ResolvePathHandler resolvePathHandler,
                                            final PathInfoHandler diskUsagePathHandler,
                                            final AttributeDefinition... attributes) {
        this(path, true, type, resolvePathHandler, diskUsagePathHandler, attributes);
    }

    protected AbstractFileHandlerDefinition(final PathElement path, final boolean registerLegacyOps,
                                            final Class<? extends Handler> type,
                                            final ResolvePathHandler resolvePathHandler,
                                            final PathInfoHandler diskUsagePathHandler,
                                            final AttributeDefinition... attributes) {
        super(path, registerLegacyOps, type, new DefaultPropertySorter(FileNameLastComparator.INSTANCE), attributes);
        this.registerLegacyOps = registerLegacyOps;
        this.resolvePathHandler = resolvePathHandler;
        this.diskUsagePathHandler = diskUsagePathHandler;
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration registration) {
        super.registerOperations(registration);
        if (registerLegacyOps) {
            registration.registerOperationHandler(new SimpleOperationDefinitionBuilder(CHANGE_FILE_OPERATION_NAME, getResourceDescriptionResolver())
                    .setDeprecated(ModelVersion.create(1, 2, 0))
                    .setParameters(CommonAttributes.FILE)
                    .build(), HandlerOperations.CHANGE_FILE);
        }
        if (resolvePathHandler != null)
            registration.registerOperationHandler(resolvePathHandler.getOperationDefinition(), resolvePathHandler);
        if (diskUsagePathHandler != null)
            PathInfoHandler.registerOperation(registration, diskUsagePathHandler);
    }

    private static class FileNameLastComparator implements Comparator<String> {
        static final FileNameLastComparator INSTANCE = new FileNameLastComparator();
        static final int EQUAL = 0;
        static final int GREATER = 1;
        static final int LESS = -1;

        private final String filePropertyName = CommonAttributes.FILE.getPropertyName();

        @Override
        public int compare(final String o1, final String o2) {
            if (o1.equals(o2)) {
                return EQUAL;
            }
            // File should always be last
            if (filePropertyName.equals(o1)) {
                return GREATER;
            }
            if (filePropertyName.equals(o2)) {
                return LESS;
            }
            return o1.compareTo(o2);
        }
    }
}
