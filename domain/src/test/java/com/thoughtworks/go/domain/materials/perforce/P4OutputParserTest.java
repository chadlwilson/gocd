/*
 * Copyright 2023 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.domain.materials.perforce;

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.command.ConsoleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class P4OutputParserTest {
    private P4OutputParser parser;
    private static final SimpleDateFormat DESCRIPTION_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private P4Client p4Client;

    @BeforeEach
    void setUp() {
        p4Client = mock(P4Client.class);
        parser = new P4OutputParser(p4Client);
    }

    @Test
    void shouldRetrieveRevisionFromChangesOutput() {
        String output = "Change 2 on 2008/08/19 by cceuser@connect4 'some modification message'";
        long revision = parser.revisionFromChange(output);
        assertThat(revision).isEqualTo(2L);
    }

    @Test
    void shouldThrowExceptionIfP4ReturnDifferentDateFormatWhenCannotParseFistLineOfP4Describe() {
        String output = "Change 2 on 08/08/19 by cceuser@connect4 'some modification message'";
        Modification modification = new Modification();
        try {
            parser.parseFistline(modification, output, new ConsoleResult(0, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        } catch (P4OutputParseException e) {
            assertThat(e.getMessage()).contains("Could not parse P4 describe:");

        }
    }

    @Test
    void shouldRetrieveModificationFromDescription() throws P4OutputParseException, ParseException {
        String output =
                "Change 2 by cce123user@connect4_10.18.2.31 on 2008/08/19 15:04:43\n"
                        + "\n"
                        + "\tAdded config file\n"
                        + "\n"
                        + "Affected files ...\n"
                        + "\n"
                        + "... //depot/cruise-config.xml#1 add\n"
                        + "... //depot/README.txt#2 edit\n"
                        + "... //depot/cruise-output/log.xml#1 delete\n"
                        + "";
        Modification mod = parser.modificationFromDescription(output, new ConsoleResult(0, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        assertThat(mod.getRevision()).isEqualTo("2");
        assertThat(mod.getUserName()).isEqualTo("cce123user@connect4_10.18.2.31");
        assertThat(mod.getModifiedTime()).isEqualTo(DESCRIPTION_FORMAT.parse("2008/08/19 15:04:43"));
        assertThat(mod.getComment()).isEqualTo("Added config file");
        List<ModifiedFile> files = mod.getModifiedFiles();
        assertThat(files.size()).isEqualTo(3);
        assertThat(files.get(0).getAction()).isEqualTo(ModifiedAction.added);
        assertThat(files.get(0).getFileName()).isEqualTo("cruise-config.xml");
        assertThat(files.get(1).getAction()).isEqualTo(ModifiedAction.modified);
        assertThat(files.get(2).getAction()).isEqualTo(ModifiedAction.deleted);
        assertThat(files.get(2).getFileName()).isEqualTo("cruise-output/log.xml");
    }

    /*
     * This test reproduces a problem we saw at a customer's installation, where the changelist was really really large
     * It caused a frequent StackOverflow in the java regex library.
     */
    @Test
    void shouldParseChangesWithLotsOfFilesWithoutError() throws IOException, P4OutputParseException {
        final StringWriter writer = new StringWriter();
        try (InputStreamReader inputStream = new InputStreamReader(new ClassPathResource("/BIG_P4_OUTPUT.txt").getInputStream(), Charset.defaultCharset())) {
            inputStream.transferTo(writer);
        }
        String output = writer.toString();
        Modification modification = parser.modificationFromDescription(output, new ConsoleResult(0, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        assertThat(modification.getModifiedFiles().size()).isEqualTo(1304);
        assertThat(modification.getModifiedFiles().get(0).getFileName()).isEqualTo("Internal Projects/ABC/Customers3/ABC/RIP/SomeProject/data/main/config/lib/java/AdvJDBCColumnHandler.jar");
    }


    @Test
    void shouldThrowExceptionWhenCannotParseChanges() {
        String line = "Some line I don't understand";
        try {
            parser.modificationFromDescription(line, new ConsoleResult(0, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
            fail("Should throw exception if can't parse the description");
        } catch (P4OutputParseException expected) {
            assertThat(expected.getMessage()).contains(line);
        }
    }

    @Test
    void shouldParseDescriptionProperly_Bug2456() throws P4OutputParseException {
        String description =
                "Change 570548 by michael@michael_AB2-ENV-WXP-000_ABcore on 2009/01/12 14:47:04\n"
                        + "\n"
                        + "\tAdd a WCF CrossDomainPolicyService and CrossDomain.xml.\n"
                        + "\tUpdate service reference.\n"
                        + "\tUpdate app.config and template config files.\n"
                        + "\n"
                        + "Affected files ...\n"
                        + "\n"
                        + "... //APP/AB/Core/Somemv.Core/Somemv.Core.Security.ServiceContracts/ICrossDomainPolicyService.cs#1 add\n"
                        + "... //APP/AB/Core/Somemv.Core/Somemv.Core.Security.ServiceContracts/Somemv.Core.Security.ServiceContracts.csproj#3"
                        + " edit\n"
                        + "... //APP/AB/Core/Somemv.Core/Somemv.Core.Security.Services/CrossDomainPolicyService.cs#1 add\n"
                        + "... //APP/AB/Core/Somemv.Core/Somemv.Core.Security.Services/Somemv.Core.Security.Services.csproj#4 edit\n"
                        + "... //APP/AB/Core/Somemv.Core/Somemv.Core.ServiceReferences/Somemv.Core.ServiceReferences.csproj#44 edit\n"
                        + "... //APP/AB/Core/Somemv.Core/Somemv.Core.ServiceReferences/Service References/SecurityService/"
                        + "SecurityService33.xsd#2 edit\n"
                        + "... //APP/AB/Core/Somemv.Core/Somemv.Core.ServiceReferences/app.config#30 edit\n"
                        + "... //APP/AB/Core/Somemv.Simulation/Somemv.Simulation.Coordinator.Services/Analysis/AnalysisServiceAdapter.cs#30"
                        + " edit\n"
                        + "... //APP/AB/Core/Somemv.Simulation/Somemv.Simulation.Services/ManageableService.cs#4 edit\n"
                        + "... //APP/AB/Core/Products/Somemv.RemotingService/App.config#102 edit\n"
                        + "... //APP/AB/Core/Products/Somemv.RemotingService/CrossDomain.xml#1 add\n"
                        + "... //APP/AB/Core/Products/Somemv.RemotingService/Somemv.RemotingService.csproj#53 edit\n"
                        + "... //APP/AB/Core/Templates/Somemv.RemotingService/app_template.config#69 edit\n"
                        + "... //APP/AB/Core/Templates/Somemv.RemotingService/coordinator_template.config#69 edit\n"
                        + "";

        Modification modification = parser.modificationFromDescription(description, new ConsoleResult(0, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        assertThat(modification.getComment()).isEqualTo("Add a WCF CrossDomainPolicyService and CrossDomain.xml.\n"
                + "Update service reference.\n"
                + "Update app.config and template config files.");

    }

    @Test
    void shouldParseCommentWithAffectedFilesCorrectly() throws P4OutputParseException {
        String description =
                "Change 5 by cceuser@CceDev01 on 2009/08/06 14:21:30\n"
                        + "\n"
                        + "\tAffected files ...\n"
                        + "\t\n"
                        + "\t... //DEPOT/FILE#943 edit\n"
                        + "\n"
                        + "Affected files ...\n"
                        + "\n"
                        + "... //depot/file#5 edit";

        Modification modification = parser.modificationFromDescription(description, new ConsoleResult(0, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        assertThat(modification.getComment()).isEqualTo("Affected files ...\n"
                + "\n"
                + "... //DEPOT/FILE#943 edit");

    }

    @Test
    void shouldParseCorrectlyWhenCommentIsEmpty() throws P4OutputParseException {
        String description =
                "Change 102 by godev@blrstdcrspair03 on 2013/06/04 12:00:35\n"
                        + "\n"
                        + "\n"
                        + "Affected files ...\n"
                        + "\n"
                        + "... //another_depot/1.txt#6 edit";

        Modification modification = parser.modificationFromDescription(description, new ConsoleResult(0, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        assertThat(modification.getComment()).isEqualTo("");
    }

    private static final String BUG_2503_OUTPUT = "Change 122636 by ipaipa@ipaipa-STANDARD-DHTML on 2009/02/06 17:53:57\n"
            + " \n"
            + "\tPCTD-820: Fix for grid header scrolling issue when the user has tabbed between the filter "
            + "input boxes.\n"
            + " \n"
            + "Jobs fixed ...\n"
            + " \n"
            + "PCTD-820 on 2009/02/09 by philipe *closed*\n"
            + " \n"
            + "\tUsing the Tab key to move through grid filters can cause grid column headers to"
            + " become mis-aligned\n"
            + " \n"
            + "Affected files ...\n"
            + "\n"
            + "... //APP/RF/Core/Somemv.Core/Somemv.Core#1 add\n";

    @Test
    void shouldMatchWhenCommentsAreMultipleLines() throws P4OutputParseException, ParseException {
        Modification modification = parser.modificationFromDescription(BUG_2503_OUTPUT, new ConsoleResult(0, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));

        assertThat(parser.revisionFromChange(BUG_2503_OUTPUT)).isEqualTo(122636L);
        assertThat(modification.getModifiedTime()).isEqualTo(DESCRIPTION_FORMAT.parse("2009/02/06 17:53:57"));
        assertThat(modification.getModifiedFiles().size()).isEqualTo(1);
        assertThat(modification.getUserName()).isEqualTo("ipaipa@ipaipa-STANDARD-DHTML");
    }

    @Test
    void shouldIgnoreEmptyLinesInChanges() {
        final String output =
                "Change 539921 on 2008/09/24 by abc@SomeRefinery_abc_sa1-sgr-xyz-001 'more work in progress on ABC un'\n"
                        + "Change 539920 on 2008/09/24 by "
                        + "abc@SomeRefinery_abc_sa1-sgr-xyz-001 'Fixed pipeline for abc-new-sale'\n"
                        + "Change 539919 on 2008/09/24 by "
                        + "james@SomeRefinery_def_sa1-sgr-xyz-001 'Assignment stage to send ok and'\n"
                        + "Change 539918 on 2008/09/24 by abc@SomeRefinery_abc_sa1-sgr-xyz-001 'More refactoring and code clean'\n"
                        + "Change 539917 on 2008/09/24 by thomas@tom_ws_stuff 'added client name for Arza SW'\n"
                        + "Change 539916 on 2008/09/24 by alex@SA2-COUNT-LAX-001 'sending the context further '\n"
                        + "Change 539915 on 2008/09/24 by ellen@ellen-box-1 'TT 678 cause:Old code try to'\n"
                        + "Change 539914 on 2008/09/24 by valerie@ExcelSheets_New 'update sheet comments '\n"
                        + "\n"
                        + "Change 539913 on 2008/09/24 by perforce@SOM-NAME-HERE-HOST-1 'apply new version numbers via A'\n"
                        + "Change 539912 on 2008/09/24 by lance@lance-box-1 'Corrected a typo. '\n"
                        + "\n"
                        + "Change 539911 on 2008/09/24 by lance@lance-box-1 'corrected a typo. '\n"
                        + "Change 539910 on 2008/09/24 by josh@josh_box_1 'Changes to remove hacks I had m'\n"
                        + "Change 539909 on 2008/09/24 by thomas@tom_ws_stuff 'Added Arza SW to add request '\n"
                        + "Change 539908 on 2008/09/24 by padma@Padma '1. Fix #2644 : When the FSOi'\n"
                        + "Change 539907 on 2008/09/24 by berlin@Dans_Box 'Added GetDocumentMetadataForEdi'\n"
                        + "Change 539906 on 2008/09/24 by lance@lance-box-1 'Added detail aboutPFP. '\n"
                        + "Change 539904 on 2008/09/24 by lance@lance-box-1 'Added a detail about PFP. '\n"
                        + "Change 539903 on 2008/09/24 by nitya@docs_box 'Updated for lam, am 20080923'\n"
                        + "Change 539902 on 2008/09/24 by abc@SomeRefinery_abc_sa1-sgr-xyz-001 'Work in progress '\n"
                        + "Change 539901 on 2008/09/24 by anil@anil-box-1 'Added all columns of AA_TASK to'\n"
                        + "";

        when(p4Client.describe(any(Long.class))).thenReturn("Change 539921 by abc@SomeRefinery_abc_sa1-sgr-xyz-001 on 2008/09/24 12:10:00\n"
                + "\n"
                + "\tmore work in progress on ABC unit test\n"
                + "\n"
                + "Affected files ...\n"
                + "");

        List<Modification> modifications = parser.modifications(new ConsoleResult(0, List.of(output.split("\n")), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        assertThat(modifications.size()).isEqualTo(20);
    }

    @Test
    void shouldIgnoreBadLinesAndLogThem() {
        try (LogFixture logging = logFixtureFor(P4OutputParser.class, Level.DEBUG)) {
            final String output = "Change 539921 on 2008/09/24 "
                    + "by abc@SomeRefinery_abc_sa1-sgr-xyz-001 'more work in progress on MDC un'\n";
            final String description = "Change that I cannot parse :-(\n";

            when(p4Client.describe(any(Long.class))).thenReturn(description);

            List<Modification> modifications = parser.modifications(new ConsoleResult(0, List.of(output.split("\n")), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
            assertThat(modifications.size()).isEqualTo(0);
            assertThat(logging.getLog()).contains(description);
        }
    }
}
