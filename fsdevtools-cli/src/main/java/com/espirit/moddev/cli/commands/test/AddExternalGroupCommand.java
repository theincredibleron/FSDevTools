/*
 *
 * *********************************************************************
 * fsdevtools
 * %%
 * Copyright (C) 2016 e-Spirit AG
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * *********************************************************************
 *
 */

package com.espirit.moddev.cli.commands.test;

import com.espirit.moddev.cli.api.result.Result;
import com.espirit.moddev.cli.commands.SimpleCommand;
import com.espirit.moddev.cli.results.SimpleResult;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;
import de.espirit.common.base.Logging;
import de.espirit.firstspirit.access.project.Group;
import de.espirit.firstspirit.access.project.Project;
import de.espirit.firstspirit.access.store.LockException;

import java.util.Optional;

import static de.espirit.common.imaging.ImageIOUtil.LOGGER;


/**
 * Command for adding a group on a FirstSpirit server.
 *
 * @author e-Spirit AG
 */
@com.github.rvesse.airline.annotations.Command(name = "add", groupNames = {"group"}, description = "Add a group")
public class AddExternalGroupCommand extends SimpleCommand {


    @Option(name = "--fsName", description = "The name the group should have")
    @Required
    private String fsName;

    @Override
    @SuppressWarnings("squid:S2221")
    public Result call() {

        Project project = getContext().getProject();

        EcsGroup ecsGroup = new EcsGroup().setfsName(fsName).setIsProjectAdmin(false);
        final Optional<Group> optionGroup = project
                .getGroups()
                .stream()
                .filter(
                        group -> group.getName().equals(ecsGroup.fsName)
                ).findFirst();
        if (!optionGroup.isPresent()) {
            try {
                project.lock();
                final Group externalGroup = project.createExternalGroup(ecsGroup.fsName, ecsGroup.externalDN);
                externalGroup.setAdmin(ecsGroup.isProjectAdmin);
                project.save();
            } catch (final LockException exception) {
                Logging.logError("Could not lock the project " + project.getName(), exception, LOGGER);
            } finally {
                Logging.logInfo("Unlocking project", LOGGER);
                project.unlock();
            }
        }

        return new SimpleResult();
    }

    private static class EcsGroup {
        public String fsName;
        public String externalDN;
        public boolean isProjectAdmin;

        public EcsGroup setfsName(String fsName) {
            this.fsName = fsName;
            return this;
        }

        public EcsGroup setIsProjectAdmin(boolean isProjectAdmin) {
            this.isProjectAdmin = isProjectAdmin;
            return this;
        }
    }
}
