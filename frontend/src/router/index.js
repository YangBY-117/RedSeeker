import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import RecommendView from '../views/RecommendView.vue';
import RouteView from '../views/RouteView.vue';
import PlacesView from '../views/PlacesView.vue';
import DiaryView from '../views/DiaryView.vue';
import DiaryDetailView from '../views/DiaryDetailView.vue';
import UserCenterView from '../views/UserCenterView.vue';

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/recommend', name: 'recommend', component: RecommendView },
  { path: '/route', name: 'route', component: RouteView },
  { path: '/places', name: 'places', component: PlacesView },
  { path: '/diary', name: 'diary', component: DiaryView },
  { path: '/diary/:id', name: 'diary-detail', component: DiaryDetailView },
  { path: '/user/center', name: 'user-center', component: UserCenterView }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
