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

angular.module('angularApp').directive('activeMenu', [ "$translate", "$locale", "tmhDynamicLocale", function($translate, $locale, tmhDynamicLocale) {
	return {
		restrict : 'A',
		link : [ "scope", "element", "attrs", function(scope, element, attrs) {
			var language = attrs.activeMenu;

			scope.$watch(function() {
				return $translate.use();
			}, function(selectedLanguage) {
				if (language === selectedLanguage) {
					tmhDynamicLocale.set(language);
					element.addClass('active');
				} else {
					element.removeClass('active');
				}
			});
		} ]
	};
} ]).directive('activeLink', [ "location", function(location) {
	return {
		restrict : 'A',
		link : [ "scope", "element", "attrs", function(scope, element, attrs) {
			var clazz = attrs.activeLink;
			var path = attrs.href;
			path = path.substring(1); // hack because path does bot return
			// including hashbang
			scope.location = location;
			scope.$watch('location.path()', function(newPath) {
				if (path === newPath) {
					element.addClass(clazz);
				} else {
					element.removeClass(clazz);
				}
			});
		} ]
	};
} ]);
