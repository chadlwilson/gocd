<#--
 * Copyright Thoughtworks, Inc.
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
 -->
<#noparse>
<textarea rows="0" cols="0" id="tab-content-of-materials-template" style="display: none;">
    {for revision in revisions}
    <div class="material_revision">
        <div class="material">
            <span class="revision_scmtype">${% revision.scmType %}</span>
            <span class="revision_location">{if revision.materialName}${% revision.materialName.concat(' - ') %} {/if}${% revision.location %}</span>
            <br/>
            {if revision.scmType=='Dependency'}
            {var modification = revision.modifications[0]}
            <div class="dependency_revision_highlight-${% revision.changed %}">
                <span class="revision_revision">
                    <a href="</#noparse>${req.getContextPath()}<#noparse>/${% revision.revision_href %}">${% revision.revision %}</a>
                </span>
                <span>${% revision.action %}</span>
                on
                <span>${% revision.date %}</span>
            </div>
            {/if}
        </div>
        {if revision.scmType != 'Dependency'}
        <table class="modifications">
            {for modification in revision.modifications}
            <tbody>
            <tr>
                <th colspan="2" class="highlight-${% revision.changed %}">

                    <span class="normal revision_information">Revision: ${% modification.revision %}, modified by ${% _.escape(modification.user) %} on ${% modification.date %} </span>
                    <br/>

                    <span title="Comment" class="comment">
                        {if revision.scmType == 'Package' }
                            {if modification.comment != null && modification.comment != ""}
                                {var comment_map = JSON.parse(modification.comment)}
                                {if comment_map.COMMENT != null}
                                    ${% comment_map.COMMENT %}<br/>
                                {/if}
                                {if comment_map.TRACKBACK_URL != null}
                                    Trackback: <a href="${% comment_map.TRACKBACK_URL %}">${% comment_map.TRACKBACK_URL %}</a>
                                {else}
                                    Trackback: Not Provided
                                {/if}
                            {/if}
                        {else}
                            "${% modification.comment.replace(/\n/g,"<br/>") %}"
                        {/if}
                    </span>
                </th>
            </tr>
            {for file in modification.modifiedFiles}
            <tr class="{if file_index%2==0} even {else} odd {/if}">
                <td title="${% file.action %}" class="${% file.action %}">
                     <span class="modified_file">${% file.fileName %}</span>
                </td>
            </tr>
            {/for}
            </tbody>
            {/for}
        </table>
        {/if}
    </div>
    {forelse}
    No materials found.
    {/for}
</textarea>
</#noparse>