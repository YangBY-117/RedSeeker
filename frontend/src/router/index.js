import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import RecommendView from '../views/RecommendView.vue';
import RouteView from '../views/RouteView.vue';
import PlacesView from '../views/PlacesView.vue';
import DiaryView from '../views/DiaryView.vue';

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/recommend', name: 'recommend', component: RecommendView },
  { path: '/route', name: 'route', component: RouteView },
  { path: '/places', name: 'places', component: PlacesView },
  { path: '/diary', name: 'diary', component: DiaryView }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
