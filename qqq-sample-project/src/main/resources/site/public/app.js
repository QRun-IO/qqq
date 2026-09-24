/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

const sampleSite = "public";
const currentRoute = document.getElementById("current-route");
const showRoute = () => { currentRoute.textContent = sampleSite + ": " + window.location.pathname; };
document.querySelector("[data-route]").addEventListener("click", event => {
   if(event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
   event.preventDefault();
   window.history.pushState(null, "", event.currentTarget.href);
   showRoute();
});
window.addEventListener("popstate", showRoute);
showRoute();
