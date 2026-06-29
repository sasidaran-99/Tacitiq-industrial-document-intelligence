import { useEffect, useRef, useState } from 'react';
import * as THREE from 'three';
import { Play, Pause, Activity } from 'lucide-react';

export default function DigitalTwin3D() {
  const mountRef = useRef<HTMLDivElement>(null);
  const [rpm, setRpm] = useState(1480);
  const [vibration, setVibration] = useState(1.8);
  const [temperature, setTemperature] = useState(45.0);
  const [isRunning, setIsRunning] = useState(true);

  // Calculate health score dynamically
  const vibrationMax = 4.5;
  const tempMax = 85.0;
  
  const vibPercent = Math.min(1.0, vibration / vibrationMax);
  const tempPercent = Math.min(1.0, (temperature - 20) / (tempMax - 20));
  
  const healthScore = Math.max(0.01, 1.0 - (vibPercent * 0.4 + tempPercent * 0.6));
  
  // Health state color mapping
  let healthColor = "text-brandEmerald";
  let healthBg = "bg-emerald-50 text-brandEmerald border-emerald-100";
  let meshColor = 0x10b981; // Emerald
  
  if (healthScore < 0.6) {
    healthColor = "text-brandRed";
    healthBg = "bg-red-50 text-brandRed border-red-100";
    meshColor = 0xef4444; // Red
  } else if (healthScore < 0.82) {
    healthColor = "text-brandAmber";
    healthBg = "bg-amber-50 text-brandAmber border-amber-100";
    meshColor = 0xf59e0b; // Amber
  }

  // Ref to update rotation speed in animate loop
  const speedRef = useRef(rpm / 1000);
  useEffect(() => {
    speedRef.current = isRunning ? rpm / 1000 : 0;
  }, [rpm, isRunning]);

  // Mesh color ref
  const colorRef = useRef(meshColor);
  useEffect(() => {
    colorRef.current = meshColor;
  }, [meshColor]);

  useEffect(() => {
    if (!mountRef.current) return;
    const currentMount = mountRef.current;

    // Dimensions
    const width = mountRef.current.clientWidth;
    const height = mountRef.current.clientHeight;

    // Scene, Camera, Renderer
    const scene = new THREE.Scene();

    const camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 1000);
    camera.position.set(0, 1.8, 4.0);

    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setSize(width, height);
    renderer.setPixelRatio(window.devicePixelRatio);
    renderer.setClearColor(0x000000, 0); // Transparent clear color
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    mountRef.current.appendChild(renderer.domElement);

    // Lighting
    const ambientLight = new THREE.AmbientLight(0xffffff, 1.2);
    scene.add(ambientLight);

    const hemiLight = new THREE.HemisphereLight(0xffffff, 0xd9e1e8, 1.0);
    scene.add(hemiLight);

    const directionalLight1 = new THREE.DirectionalLight(0xffffff, 2.0);
    directionalLight1.position.set(6, 10, 5);
    directionalLight1.castShadow = true;
    directionalLight1.shadow.mapSize.width = 2048;
    directionalLight1.shadow.mapSize.height = 2048;
    directionalLight1.shadow.bias = -0.001;
    scene.add(directionalLight1);

    // Build Industrial Pump geometries
    const pumpGroup = new THREE.Group();

    // Pump Base Plate
    const baseGeo = new THREE.BoxGeometry(4, 0.2, 1.8);
    const baseMat = new THREE.MeshStandardMaterial({ color: 0xe2e8f0, roughness: 0.4, metalness: 0.2 });
    const basePlate = new THREE.Mesh(baseGeo, baseMat);
    basePlate.position.y = -0.1;
    basePlate.castShadow = true;
    basePlate.receiveShadow = true;
    pumpGroup.add(basePlate);

    // Motor Block (Cylinder) - Styled Azure
    const motorGeo = new THREE.CylinderGeometry(0.6, 0.6, 1.8, 32);
    const motorMat = new THREE.MeshStandardMaterial({ color: 0x0ea5e9, metalness: 0.8, roughness: 0.15 });
    const motor = new THREE.Mesh(motorGeo, motorMat);
    motor.rotation.z = Math.PI / 2;
    motor.position.set(-1.0, 0.6, 0);
    motor.castShadow = true;
    motor.receiveShadow = true;
    pumpGroup.add(motor);

    // Inboard Bearing Housing (Interactive component)
    const bearingGeo = new THREE.CylinderGeometry(0.45, 0.45, 0.6, 32);
    const bearingMat = new THREE.MeshStandardMaterial({ color: 0x64748b, metalness: 0.7, roughness: 0.2 });
    const bearing = new THREE.Mesh(bearingGeo, bearingMat);
    bearing.rotation.z = Math.PI / 2;
    bearing.position.set(0.2, 0.6, 0);
    bearing.castShadow = true;
    bearing.receiveShadow = true;
    pumpGroup.add(bearing);

    // Rotating Coupling Shaft (Spins) - Polished Chrome
    const shaftGeo = new THREE.CylinderGeometry(0.12, 0.12, 0.7, 16);
    const shaftMat = new THREE.MeshStandardMaterial({ color: 0xf8fafc, metalness: 0.95, roughness: 0.05 });
    const shaft = new THREE.Mesh(shaftGeo, shaftMat);
    shaft.rotation.z = Math.PI / 2;
    shaft.position.set(0.85, 0.6, 0);
    shaft.castShadow = true;
    shaft.receiveShadow = true;

    // Add a visible off-center coupling keyway/bolt block to show rotation
    const boltGeo = new THREE.BoxGeometry(0.15, 0.25, 0.25);
    const boltMat = new THREE.MeshStandardMaterial({ color: 0x475569, metalness: 0.8 });
    const bolt = new THREE.Mesh(boltGeo, boltMat);
    bolt.position.set(0, 0.12, 0);
    bolt.castShadow = true;
    bolt.receiveShadow = true;
    shaft.add(bolt);

    pumpGroup.add(shaft);

    // Impeller Pump Volute casing - Styled Emerald
    const voluteGeo = new THREE.TorusGeometry(0.7, 0.25, 16, 100);
    const voluteMat = new THREE.MeshStandardMaterial({ color: 0x10b981, metalness: 0.8, roughness: 0.15 });
    const volute = new THREE.Mesh(voluteGeo, voluteMat);
    volute.position.set(1.5, 0.6, 0);
    volute.castShadow = true;
    volute.receiveShadow = true;
    pumpGroup.add(volute);

    // Discharge pipe
    const pipeGeo = new THREE.CylinderGeometry(0.22, 0.22, 1.2, 16);
    const pipeMat = new THREE.MeshStandardMaterial({ color: 0x94a3b8, metalness: 0.8, roughness: 0.2 });
    const pipe = new THREE.Mesh(pipeGeo, pipeMat);
    pipe.position.set(1.5, 1.6, 0);
    pipe.castShadow = true;
    pipe.receiveShadow = true;
    pumpGroup.add(pipe);

    // Scale pump model up by approximately 20%
    pumpGroup.scale.set(1.2, 1.2, 1.2);
    scene.add(pumpGroup);

    // Showcase Pedestal Platform
    const pedestalGeo = new THREE.CylinderGeometry(2.5, 2.7, 0.6, 64);
    const pedestalMat = new THREE.MeshStandardMaterial({
      color: 0xE2E8F0,
      roughness: 0.25,
      metalness: 0.1
    });
    const pedestal = new THREE.Mesh(pedestalGeo, pedestalMat);
    pedestal.position.y = -0.5;
    pedestal.castShadow = true;
    pedestal.receiveShadow = true;
    scene.add(pedestal);

    // Showcase Floor
    const floorGeo = new THREE.PlaneGeometry(100, 100);
    const floorMat = new THREE.MeshStandardMaterial({
      color: "#D9E1E8",
      roughness: 0.9,
      metalness: 0.05
    });
    const floor = new THREE.Mesh(floorGeo, floorMat);
    floor.rotation.x = -Math.PI / 2;
    floor.position.y = -0.8;
    floor.receiveShadow = true;
    scene.add(floor);

    // Set camera target
    camera.lookAt(0, 0.3, 0);

    // Animation Loop
    let animationFrameId: number;
    const animate = () => {
      animationFrameId = requestAnimationFrame(animate);

      // Rotate shaft based on RPM
      if (shaft) {
        shaft.rotation.x += speedRef.current;
      }

      // Pulse color of the bearing housing depending on active mesh health color
      if (bearingMat) {
        bearingMat.color.setHex(colorRef.current);
      }

      // Continuous scene rotation for depth
      pumpGroup.rotation.y = (Date.now() * 0.0004);

      renderer.render(scene, camera);
    };

    animate();

    // Resize Handler
    const handleResize = () => {
      if (!mountRef.current) return;
      const w = mountRef.current.clientWidth;
      const h = mountRef.current.clientHeight;
      camera.aspect = w / h;
      camera.updateProjectionMatrix();
      renderer.setSize(w, h);
    };
    window.addEventListener('resize', handleResize);

    // Clean up
    return () => {
      cancelAnimationFrame(animationFrameId);
      window.removeEventListener('resize', handleResize);
      if (currentMount && renderer.domElement) {
        currentMount.removeChild(renderer.domElement);
      }
      baseGeo.dispose();
      baseMat.dispose();
      motorGeo.dispose();
      motorMat.dispose();
      bearingGeo.dispose();
      bearingMat.dispose();
      shaftGeo.dispose();
      shaftMat.dispose();
      voluteGeo.dispose();
      voluteMat.dispose();
      pipeGeo.dispose();
      pipeMat.dispose();
      pedestalGeo.dispose();
      pedestalMat.dispose();
      floorGeo.dispose();
      floorMat.dispose();
      renderer.dispose();
    };
  }, []);

  return (
    <div className="h-full w-full flex flex-col md:flex-row relative bg-transparent">
      {/* 3D View Container with Studio Spotlight Background */}
      <div
        ref={mountRef}
        className="flex-1 h-full min-h-[300px]"
        style={{
          background: 'radial-gradient(circle at 50% 50%, rgba(59, 130, 246, 0.10) 0px, transparent 600px), linear-gradient(180deg, #F7FAFC 0%, #EAF2F9 100%)'
        }}
      />

      {/* Floating State Indicators Overlay */}
      <div className="absolute top-6 left-6 p-5 glass-panel rounded-2xl pointer-events-none flex flex-col gap-3 shadow-premium">
        <span className="text-[9px] text-slate-400 font-extrabold uppercase tracking-widest">
          Digital Twin State
        </span>
        <div className="flex gap-4">
          <div>
            <p className="text-[9px] text-slate-500 font-bold uppercase tracking-wider">Asset Tag</p>
            <p className="text-xs font-bold text-slate-800">Pump P-101</p>
          </div>
          <div>
            <p className="text-[9px] text-slate-500 font-bold uppercase tracking-wider">Health Index</p>
            <span className={`text-xs font-extrabold ${healthColor}`}>
              {Math.round(healthScore * 100)}%
            </span>
          </div>
        </div>
        <div className={`border p-2.5 rounded-xl flex items-center gap-3 shadow-sm ${healthBg}`}>
          <Activity className="h-4.5 w-4.5" />
          <div>
            <p className="text-[10px] font-bold uppercase tracking-wider">Status</p>
            <p className="text-[9px] opacity-90 leading-normal font-semibold">
              {healthScore > 0.82 ? "Normal operations baseline" : healthScore > 0.6 ? "Minor misalignment warning" : "Critical failure shutdown threshold"}
            </p>
          </div>
        </div>
      </div>

      {/* Controls Sidebar Dashboard */}
      <div className="w-full md:w-80 bg-white/70 backdrop-blur-md border-t md:border-t-0 md:border-l border-slate-200/50 p-6 flex flex-col justify-between z-10">
        <div>
          <h3 className="text-xs font-extrabold uppercase tracking-wider text-slate-700 mb-4">
            Twin Parameters
          </h3>

          <div className="space-y-6">
            {/* Speed controller */}
            <div>
              <div className="flex justify-between text-xs font-bold text-slate-500 mb-2 uppercase tracking-wider">
                <span>Rotational Speed</span>
                <span className="text-brandAzure font-bold font-mono">{rpm} RPM</span>
              </div>
              <input
                type="range"
                min="0"
                max="3600"
                step="50"
                value={rpm}
                onChange={e => setRpm(Number(e.target.value))}
                className="w-full h-1 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-brandAzure"
              />
            </div>

            {/* Vibration controller */}
            <div>
              <div className="flex justify-between text-xs font-bold text-slate-500 mb-2 uppercase tracking-wider">
                <span>Vibration RMS</span>
                <span className="text-brandRed font-bold font-mono">{vibration} mm/s</span>
              </div>
              <input
                type="range"
                min="0.1"
                max="6.0"
                step="0.1"
                value={vibration}
                onChange={e => setVibration(Number(e.target.value))}
                className="w-full h-1 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-brandRed"
              />
            </div>

            {/* Temperature controller */}
            <div>
              <div className="flex justify-between text-xs font-bold text-slate-500 mb-2 uppercase tracking-wider">
                <span>Bearing Temp</span>
                <span className="text-brandAmber font-bold font-mono">{temperature} °C</span>
              </div>
              <input
                type="range"
                min="20"
                max="100"
                step="1"
                value={temperature}
                onChange={e => setTemperature(Number(e.target.value))}
                className="w-full h-1 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-brandAmber"
              />
            </div>
          </div>
        </div>

        {/* Play/Pause simulations */}
        <div className="pt-5 border-t border-slate-200/50 mt-6 flex gap-3">
          <button
            onClick={() => setIsRunning(prev => !prev)}
            className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl font-bold text-xs transition-all duration-200 cursor-pointer shadow-sm ${
              isRunning
                ? 'bg-white hover:bg-slate-50 text-slate-700 border border-slate-200'
                : 'bg-brandEmerald text-white hover:opacity-95 hover:shadow-hover-glow'
            }`}
          >
            {isRunning ? (
              <>
                <Pause className="h-3.5 w-3.5" /> Pause Engine
              </>
            ) : (
              <>
                <Play className="h-3.5 w-3.5" /> Run Simulation
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
