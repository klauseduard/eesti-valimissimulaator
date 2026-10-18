import { mount } from 'svelte';
import './app.css';
import Metoodika from './Metoodika.svelte';

const leht = mount(Metoodika, { target: document.getElementById('metoodika')! });

export default leht;
