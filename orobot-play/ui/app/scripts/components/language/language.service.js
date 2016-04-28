/*
* Copyright 2015 Kynetics SRL
*
* This file is part of orobot.
*
* orobot is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* orobot is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with orobot.  If not, see <http://www.gnu.org/licenses/>.
*/
'use strict';

angular.module('angularApp').factory('Language', [ "$q", "$http", "$translate", "LANGUAGES", function($q, $http, $translate, LANGUAGES) {
	return {
		getCurrent : function() {
			var deferred = $q.defer();
			var language = $translate.storage().get('NG_TRANSLATE_LANG_KEY');

			if (angular.isUndefined(language)) {
				language = 'it';
			}

			deferred.resolve(language);
			return deferred.promise;
		},
		getAll : function() {
			var deferred = $q.defer();
			deferred.resolve(LANGUAGES);
			return deferred.promise;
		}
	};
} ])
/*
 * Languages codes are ISO_639-1 codes, see
 * http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes They are written in
 * English to avoid character encoding issues (not a perfect solution)
 */
.constant('LANGUAGES', [ 'it'
// Add new languages here
]);
