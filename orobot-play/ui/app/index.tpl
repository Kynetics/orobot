{*
 Copyright 2015 Kynetics SRL

 This file is part of orobot.

 orobot is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 orobot is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with orobot.  If not, see <http://www.gnu.org/licenses/>.
*}

<!doctype html>
<!--[if lt IE 7]>
<html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>
<html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>
<html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!-->
<html class="no-js">
  <!--<![endif]-->
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>Provider</title>
    <meta name="description" content="">
    <meta name="viewport" content="width=device-width">
    <!-- Place favicon.ico and apple-touch-icon.png in the root directory -->
    <!-- build:css styles/vendor.css -->
    <!-- bower:css -->
    <link rel="stylesheet"
          href="bower_components/eonasdan-bootstrap-datetimepicker/build/css/bootstrap-datetimepicker.css"/>
    <!-- endbower -->
    <!-- endbuild -->
    <!-- build:css({.tmp,app}) styles/main.css -->
    <link rel="stylesheet" href="styles/main.css">
    <link rel="stylesheet" href="styles/video.css">
    <link rel="stylesheet" href="styles/easyrtc_1.0.11.css">
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
    <!-- endbuild -->
  </head>


  <body ng-app="angularApp" layout="row" ng-controller="GlobalController as GlobalCtrl">
    <toast></toast>
    <!--[if lt IE 10]>
    <p class="browserupgrade">You are using an <strong>outdated</strong> browser. Please <a
      href="http://browsehappy.com/">upgrade your browser</a> to improve your experience.</p>
    <![endif]-->
    <!--div ng-show="ENV === 'dev'" ng-cloak="">
            <button class="btn btn-info development" onClick="javascript:window.location.href='index.html?nobackend'">No backend</button>
        </div-->
    <md-sidenav  flex ng-cloak ui-view="navbar"
                class="site-sidenav md-sidenav-left md-whiteframe-z2"
                md-component-id="left"
                ng-click="GlobalCtrl.openLeftMenu()"
                aria-label="Show User List"
                md-is-locked-open="$mdMedia('gt-sm')"></md-sidenav>
    <div flex layout="column" tabIndex="-1" role="main" class="md-whiteframe-z2">
      <md-toolbar layout="row" class="md-whiteframe-z1">
        <md-button ng-hide="$mdMedia('gt-sm')" id="main" class="menu" aria-label="Show User List">
          <md-icon md-svg-icon="/scripts/assets/images/icon/ic_menu_white_48px.svg"
                   ng-click="GlobalCtrl.openLeftMenu()"></md-icon>
        </md-button>
        <h3 ui-view="title"></h3>
      </md-toolbar>
      <md-content layout-padding ui-view="content" id="content" class="content"></md-content>
    </div>

    <!-- This section is automatically compiled by Grunt wiredep -->
    <!-- build:js scripts/vendor.js -->
    <!-- bower:js -->
    <!-- endbower -->
    <script src="lib/satellizer.js"></script>
    <!-- endbuild -->
    <!-- build:js({.tmp,app}) scripts/scripts.js -->
    <script src="scripts/app.js"></script>
    <script src="scripts/socket.io_0.9.16.js"></script>
    <script src="scripts/easyrtc_1.0.11.js"></script>
    <script src="scripts/app.external.libraries.js"></script>
    <script src="scripts/app.myconstants.js"></script>
    <script src="scripts/global.controller.js"></script>
    <!-- This section is automatically compiled by Grunt includeSource -->
    <!-- include: "type": "js", "files": "scripts/components/**/*.js" -->
    <!-- include: "type": "js", "files": "scripts/app/account/**/*.js" -->
    <!-- include: "type": "js", "files": "scripts/app/admin/**/*.js" -->
    <!-- include: "type": "js", "files": "scripts/app/entities/**/*.js" -->
    <!-- include: "type": "js", "files": "scripts/app/error/**/*.js" -->
    <!-- include: "type": "js", "files": "scripts/app/main/**/*.js" -->
    <!-- endbuild -->
  </body>
</html>
