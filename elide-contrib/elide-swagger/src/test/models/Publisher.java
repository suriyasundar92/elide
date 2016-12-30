/*
 * Copyright (c) 2016 Yahoo! Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. See accompanying LICENSE file.
 */

package test.models;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.Set;

/**
* Created by klish on 12/30/16.
*/
@Entity
@Include(rootLevel = true)
public class Publisher {

    @OneToMany
    public Set<Book> getBooks() {
        return null;
    }

    @OneToMany
    public Set<Author> getExclusiveAuthors() {
        return null;
    }
}
