import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { MeshoptDecoder } from 'three/examples/jsm/libs/meshopt_decoder.module.js';

export class HeroScene {
  private canvas3d!: HTMLCanvasElement;
  private canvasSparkler!: HTMLCanvasElement;
  private canvasMobile!: HTMLCanvasElement;
  private mouseGlowEl: HTMLElement | null = null;

  private scene!: THREE.Scene;
  private camera!: THREE.PerspectiveCamera;
  private renderer!: THREE.WebGLRenderer;
  private ambientLight!: THREE.AmbientLight;
  private cyanDirLight!: THREE.DirectionalLight;
  private purpleDirLight!: THREE.DirectionalLight;
  private cyanSoftLight!: THREE.DirectionalLight;
  private whiteRimLight!: THREE.DirectionalLight;
  private innerCyanGlow!: THREE.PointLight;
  private waveLight!: THREE.PointLight;
  private cursorLight!: THREE.PointLight;
  private sparkSprite!: THREE.Sprite;
  private particleSystem!: THREE.Points;
  private pGeometry!: THREE.BufferGeometry;

  // Group nodes
  private tiltGroup = new THREE.Group();
  private spinGroup = new THREE.Group();
  private hitSphere!: THREE.Mesh;
  private modelGroup = new THREE.Group();

  // State
  private baseScale = 1.0;
  private modelLoading = false;
  private meshes: THREE.Mesh[] = [];
  private waveShaders: any[] = [];
  private isLightTheme = false;
  private isHovered = false;
  private lastHoverState = false;
  private hoverScale = 1.0;
  private waveActive = false;
  private waveProgress = 0.0;
  private tickFrame = 0;
  private startTime = 0;
  private isDestroyed = false;
  private animationFrameId?: number;
  private sparklerAnimationFrameId?: number;
  private mobileNodesAnimationFrameId?: number;

  // Particle pool
  private particleCount = 300;
  private particles: any[] = [];
  private pIndex = 0;

  // Mouse & raycasting physics
  private mouseX = 0;
  private mouseY = 0;
  private targetMouseX = 0;
  private targetMouseY = 0;
  private raycaster = new THREE.Raycaster();
  private mouseVector = new THREE.Vector2();
  private mouseDirty = false;
  private lastMouseX = 0;
  private lastMouseY = 0;
  private mouseVelX = 0;
  private mouseVelY = 0;
  private lastHitObject: any = null;
  private lastHitPoint = new THREE.Vector3();
  private lastHitNormal = new THREE.Vector3();
  private projectedCursor3D = new THREE.Vector3();
  private tempV = new THREE.Vector3();
  private rayDir = new THREE.Vector3();
  private interpPos = new THREE.Vector3();
  private interpOffset = new THREE.Vector3();
  private particlePull = new THREE.Vector3();
  private hitPoint = new THREE.Vector3();
  private hitNormal = new THREE.Vector3();
  private localHitPoint = new THREE.Vector3();
  private lightPos = new THREE.Vector3();
  private sparkPos = new THREE.Vector3();
  private normalMatrix = new THREE.Matrix3();

  // Colors
  private colorCyan = new THREE.Color('#00ffcc');
  private colorPurple = new THREE.Color('#a85cfc');
  private colorMagenta = new THREE.Color('#ff00aa');
  private currentColor = new THREE.Color();
  private emissiveIdle = new THREE.Color('#021209');
  private emissiveHover = new THREE.Color('#011c16');
  private waveColorStart = new THREE.Color('#00ffcc');
  private waveColorEnd = new THREE.Color('#ff00aa');

  // Sparkler state
  private mouseHistory: any[] = [];
  private maxHistory = 35;
  private maxLife = 8;
  private sparkCount = 400;
  private sparks: any[] = [];
  private sIndex = 0;
  private lastX = 0;
  private lastY = 0;
  private mouseMoved = false;
  private sparklerStartTime = 0;

  // Mobile nodes
  private mobileParticles: any[] = [];
  private mobileParticleCount = 45;
  private mobileMaxDistance = 90;
  private mobileMouse = { x: -1000, y: -1000, radius: 100 };

  private engineStarting = false;
  private engineStartTimer = 0;
  
  private processing = false;
  private processingTimer = 0;
  private smoothedTorque = 0;
  
  private engineIdling = false;
  private idleTimer = 0;

  // Listeners list for easy cleanup
  private listeners: { target: EventTarget; type: string; handler: any; options?: any }[] = [];

  constructor() {}

  public triggerEngineStart(): void {
    this.engineStarting = true;
    this.engineStartTimer = 0;
  }
  
  public triggerProcessing(): void {
    this.processing = true;
    this.processingTimer = 0;
  }

  public init(
    canvas3d: HTMLCanvasElement,
    canvasSparkler: HTMLCanvasElement,
    canvasMobile: HTMLCanvasElement,
    mouseGlowEl: HTMLElement | null
  ): void {
    this.canvas3d = canvas3d;
    this.canvasSparkler = canvasSparkler;
    this.canvasMobile = canvasMobile;
    this.mouseGlowEl = mouseGlowEl;

    this.startTime = performance.now();
    this.sparklerStartTime = performance.now();

    // Initialize systems
    this.initThreeJSCore();
    this.initCursorGlow();
    this.initMobileNodes();

    // Trigger initial theme update
    this.updateSceneTheme(this.isLightTheme);
  }

  public setTheme(isLight: boolean): void {
    this.isLightTheme = isLight;
    this.updateSceneTheme(isLight);
  }

  public destroy(): void {
    this.isDestroyed = true;

    // Remove event listeners
    this.listeners.forEach((l) => {
      l.target.removeEventListener(l.type, l.handler, l.options);
    });
    this.listeners = [];

    // Cancel animation loops
    if (this.animationFrameId !== undefined) {
      cancelAnimationFrame(this.animationFrameId);
    }
    if (this.mobileNodesAnimationFrameId !== undefined) {
      cancelAnimationFrame(this.mobileNodesAnimationFrameId);
    }

    // Clean up Three.js resources
    this.meshes.forEach((mesh) => {
      if (mesh.geometry) mesh.geometry.dispose();
      if (mesh.material) {
        const mats = Array.isArray(mesh.material) ? mesh.material : [mesh.material];
        mats.forEach((m: any) => {
          m.dispose();
          if (m.map) m.map.dispose();
          if (m.normalMap) m.normalMap.dispose();
          if (m.roughnessMap) m.roughnessMap.dispose();
          if (m.metalnessMap) m.metalnessMap.dispose();
        });
      }
    });

    if (this.hitSphere) {
      this.hitSphere.geometry.dispose();
      if (Array.isArray(this.hitSphere.material)) {
        this.hitSphere.material.forEach((m) => m.dispose());
      } else {
        this.hitSphere.material.dispose();
      }
    }

    if (this.pGeometry) this.pGeometry.dispose();

    this.sparkSprite.material.dispose();
    if (this.sparkSprite.material.map) this.sparkSprite.material.map.dispose();

    if (this.renderer) {
      this.renderer.dispose();
    }
  }

  private addListener(target: EventTarget, type: string, handler: any, options?: any): void {
    target.addEventListener(type, handler, options);
    this.listeners.push({ target, type, handler, options });
  }

  // Procedural Canvas Spark Particle texture helper (glowing cyan stardust)
  private createGlowTexture(): THREE.CanvasTexture {
    const canvas = document.createElement('canvas');
    canvas.width = 16;
    canvas.height = 16;
    const ctx = canvas.getContext('2d');
    if (ctx) {
      const grad = ctx.createRadialGradient(8, 8, 0, 8, 8, 8);
      grad.addColorStop(0, 'rgba(255, 255, 255, 1)');
      grad.addColorStop(0.35, 'rgba(0, 255, 204, 0.7)');
      grad.addColorStop(1, 'rgba(0, 255, 204, 0)');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, 16, 16);
    }
    return new THREE.CanvasTexture(canvas);
  }

  /* ==========================================================================
     1. HIGH-FIDELITY THREE.JS GLB RENDER SYSTEM
     ========================================================================== */
  private initThreeJSCore(): void {
    const canvas = this.canvas3d;

    // Create Scene, Camera, and WebGLRenderer
    this.scene = new THREE.Scene();
    
    this.camera = new THREE.PerspectiveCamera(45, canvas.clientWidth / canvas.clientHeight, 0.1, 100);
    this.camera.position.set(0, 0, 4.0);

    // Detect weak hardware to disable expensive features (MSAA, high pixel ratio)
    const isLowEnd = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) || 
                      (navigator.hardwareConcurrency !== undefined && navigator.hardwareConcurrency <= 4);
    const PIXEL_RATIO_CAP = isLowEnd ? 1.0 : 2.0;

    this.renderer = new THREE.WebGLRenderer({
      canvas: canvas,
      antialias: !isLowEnd, // Skip MSAA on low-end
      alpha: true, // Transparent context to overlay HTML/CSS green wave backdrops
      powerPreference: "high-performance"
    });
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, PIXEL_RATIO_CAP));
    this.renderer.setSize(canvas.clientWidth, canvas.clientHeight, false);
    this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
    this.renderer.toneMappingExposure = 1.35;

    // Setup Consolidated Cinematic PBR Studio Lights (Optimized for 60+ FPS)
    this.ambientLight = new THREE.AmbientLight('#010705', 1.5);
    this.scene.add(this.ambientLight);

    // Key Cyan Spotlight (shines upwards from bottom-left front)
    this.cyanDirLight = new THREE.DirectionalLight('#00ffcc', 8.5);
    this.cyanDirLight.position.set(-3.0, -1.8, 2.5);
    this.scene.add(this.cyanDirLight);

    // Fill Purple Spotlight (shines from right / center-right front)
    this.purpleDirLight = new THREE.DirectionalLight('#b82cff', 7.5);
    this.purpleDirLight.position.set(3.0, -0.5, 2.0);
    this.scene.add(this.purpleDirLight);

    this.cyanSoftLight = new THREE.DirectionalLight('#00ffcc', 2.8);
    this.cyanSoftLight.position.set(-1.5, 3.5, 2.0);
    this.scene.add(this.cyanSoftLight);

    this.whiteRimLight = new THREE.DirectionalLight('#ffffff', 2.5);
    this.whiteRimLight.position.set(0.0, 4.0, 1.0);
    this.scene.add(this.whiteRimLight);

    // Single Center Glowing Point Light (Nested inside the core cavities)
    this.innerCyanGlow = new THREE.PointLight('#00ffcc', 15.0, 8.0);
    this.innerCyanGlow.position.set(0, 0, 0);
    this.scene.add(this.innerCyanGlow);

    // Dynamic Hover Wave Light
    this.waveLight = new THREE.PointLight('#00ffcc', 0.0, 5.0);
    this.waveLight.position.set(0, 2.0, 0.5);
    this.scene.add(this.waveLight);

    // Dynamic Cursor Spot Point Light (Shines directly on the mesh under the cursor)
    this.cursorLight = new THREE.PointLight('#00ffcc', 0.0, 4.5);
    this.scene.add(this.cursorLight);

    // A brilliant 3D billboard flare at the raycast intersection point
    const sparkSpriteMaterial = new THREE.SpriteMaterial({
      map: this.createGlowTexture(),
      color: '#00ffcc',
      transparent: true,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    });
    this.sparkSprite = new THREE.Sprite(sparkSpriteMaterial);
    this.sparkSprite.scale.set(0.0, 0.0, 1.0); // Hidden initially
    this.scene.add(this.sparkSprite);

    // Sparkling Stardust Particle System (Custom Shader Trail following cursor)
    this.pGeometry = new THREE.BufferGeometry();
    const pPositions = new Float32Array(this.particleCount * 3);
    const pSizes = new Float32Array(this.particleCount);
    const pAlphas = new Float32Array(this.particleCount);
    
    // Set all off-screen initially
    for (let i = 0; i < this.particleCount; i++) {
      pPositions[i*3] = 999.0;
      pPositions[i*3+1] = 999.0;
      pPositions[i*3+2] = 999.0;
      pSizes[i] = 0.0;
      pAlphas[i] = 0.0;
    }
    
    this.pGeometry.setAttribute('position', new THREE.BufferAttribute(pPositions, 3));
    this.pGeometry.setAttribute('aSize', new THREE.BufferAttribute(pSizes, 1));
    this.pGeometry.setAttribute('aAlpha', new THREE.BufferAttribute(pAlphas, 1));
    
    const pMaterial = new THREE.ShaderMaterial({
      uniforms: {
        pointTexture: { value: this.createGlowTexture() }
      },
      vertexShader: `
        attribute float aSize;
        attribute float aAlpha;
        varying float vAlpha;
        void main() {
          vAlpha = aAlpha;
          vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
          gl_PointSize = aSize * (350.0 / -mvPosition.z);
          gl_Position = projectionMatrix * mvPosition;
        }
      `,
      fragmentShader: `
        uniform sampler2D pointTexture;
        varying float vAlpha;
        void main() {
          vec4 texColor = texture2D(pointTexture, gl_PointCoord);
          gl_FragColor = texColor * vec4(1.0, 1.0, 1.0, vAlpha);
        }
      `,
      transparent: true,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    });
    
    this.particleSystem = new THREE.Points(this.pGeometry, pMaterial);
    this.scene.add(this.particleSystem);
    
    // Particle metadata pool
    for (let i = 0; i < this.particleCount; i++) {
      this.particles.push({
        active: false,
        pos: new THREE.Vector3(999, 999, 999),
        vel: new THREE.Vector3(0, 0, 0),
        age: 0,
        life: 0,
        size: 0.0,
        alpha: 1.0
      });
    }

    // Structural rotation groups to separate spin from tilt-lag
    this.tiltGroup.add(this.spinGroup);
    this.scene.add(this.tiltGroup);
   
    // Hidden sphere for extremely fast raycasting (replaces polygon raycasting)
    const hitSphereGeo = new THREE.SphereGeometry(1.6, 16, 16);
    const hitSphereMat = new THREE.MeshBasicMaterial({ visible: false });
    this.hitSphere = new THREE.Mesh(hitSphereGeo, hitSphereMat);
    this.spinGroup.add(this.hitSphere);
   
    this.spinGroup.add(this.modelGroup);

    // Initial setup for theme and model trigger
    this.updateSceneTheme(this.isLightTheme);

    // Pointer move listener on window
    this.addListener(window, 'mousemove', (e: MouseEvent) => {
      this.mouseDirty = true;
      this.targetMouseX = (e.clientX / window.innerWidth) * 2.0 - 1.0;
      this.targetMouseY = (e.clientY / window.innerHeight) * 2.0 - 1.0;

      // Raycast normalized vector mapping [X: -1 to 1, Y: 1 to -1]
      this.mouseVector.x = (e.clientX / window.innerWidth) * 2.0 - 1.0;
      this.mouseVector.y = -(e.clientY / window.innerHeight) * 2.0 + 1.0;

      // Calculate mouse velocity in normalized viewport coordinates
      this.mouseVelX = this.targetMouseX - this.lastMouseX;
      this.mouseVelY = this.targetMouseY - this.lastMouseY;

      this.lastMouseX = this.targetMouseX;
      this.lastMouseY = this.targetMouseY;
    });

    // Pointer down ripple click emitter directly on the canvas element!
    this.addListener(canvas, 'pointerdown', (e: PointerEvent) => {
      this.mouseVector.x = (e.clientX / window.innerWidth) * 2.0 - 1.0;
      this.mouseVector.y = -(e.clientY / window.innerHeight) * 2.0 + 1.0;
      this.raycaster.setFromCamera(this.mouseVector, this.camera);

      if (this.modelGroup.children.length > 0) {
        // Intersect actual model polygons for precise click effect
        const intersects = this.raycaster.intersectObjects(this.modelGroup.children, true);
        if (intersects.length > 0) {
          const hit = intersects[0];
          this.waveActive = true;
          this.waveProgress = 0.0;

          const activeMesh = this.modelGroup.children[0];
          this.localHitPoint.copy(hit.point);
          activeMesh.worldToLocal(this.localHitPoint);

          for (let i = 0; i < this.meshes.length; i++) {
            const mat = this.meshes[i].material as any;
            const sh = mat?.userData?.['shader'];
            if (sh) {
              sh.uniforms.u_intersectPoint.value.copy(this.localHitPoint);
              sh.uniforms.u_waveActive.value = 1.0;
              sh.uniforms.u_waveProgress.value = 0.0;
            }
          }

          // Stardust explosion from click point
          const hitNormalLocal = hit.face ? hit.face.normal.clone() : new THREE.Vector3(0,0,1);
          const nMat = new THREE.Matrix3().getNormalMatrix(hit.object.matrixWorld);
          const hitNormalWorld = hitNormalLocal.applyMatrix3(nMat).normalize();
          for (let k = 0; k < 25; k++) {
            const sparkVel = hitNormalWorld.clone()
              .add(new THREE.Vector3(
                (Math.random() - 0.5) * 0.75,
                (Math.random() - 0.5) * 0.75,
                (Math.random() - 0.5) * 0.75
              ))
              .normalize()
              .multiplyScalar(0.06 + Math.random() * 0.12);
            this.emitStardust(hit.point, -sparkVel.x / 0.15, -sparkVel.y / 0.15);
          }
        }
      }
    });

    // Resize listener
    this.addListener(window, 'resize', () => {
      this.camera.aspect = canvas.clientWidth / canvas.clientHeight;
      this.camera.updateProjectionMatrix();
      this.renderer.setSize(canvas.clientWidth, canvas.clientHeight, false);
    });

    // Start tick
    this.tick();
  }

  private emitStardust(origin: THREE.Vector3, velX = 0, velY = 0): void {
    const p = this.particles[this.pIndex];
    p.active = true;
    p.pos.copy(origin);
    
    // Inject subtle random noise
    p.pos.x += (Math.random() - 0.5) * 0.08;
    p.pos.y += (Math.random() - 0.5) * 0.08;
    p.pos.z += (Math.random() - 0.5) * 0.08;
    
    p.age = 0;
    p.life = 35 + Math.floor(Math.random() * 25); // Life span (35 - 60 frames)
    
    // Propel backwards + add drift noise
    p.vel.set(
      -velX * 0.15 + (Math.random() - 0.5) * 0.015,
      -velY * 0.15 + (Math.random() - 0.5) * 0.015,
      (Math.random() - 0.5) * 0.012
    );
    
    p.size = 0.07 + Math.random() * 0.06;
    p.alpha = 1.0;
    
    this.pIndex = (this.pIndex + 1) % this.particleCount;
  }

  private applyThemeToModel(isLight: boolean): void {
    this.meshes.forEach((mesh) => {
      const mat = mesh.material as THREE.MeshPhysicalMaterial;
      if (!mat) return;
      if (isLight) {
        mat.color.set('#c8f0ea'); // Soft crystal teal/cyan base
        mat.metalness = 0.18;
        mat.roughness = 0.12;
        mat.transmission = 0.80;
        mat.ior = 1.85;
        mat.thickness = 2.0;
        mat.clearcoat = 1.0;
        mat.clearcoatRoughness = 0.05;
        mat.iridescence = 0.85;
        mat.iridescenceIOR = 1.8;
        mat.emissive.set('#000000');
        mat.transparent = true;
        mat.opacity = 1.0;
        mat.map = null;
        mat.normalMap = mesh.userData['originalNormalMap'];
        mat.roughnessMap = null;
        mat.metalnessMap = null;
      } else {
        mat.color.set('#031210');
        mat.metalness = 0.98;
        mat.roughness = 0.08;
        mat.transmission = 0.0; // Solid dark metal chrome
        mat.ior = 1.5;
        mat.thickness = 0.0;
        mat.clearcoat = 1.0;
        mat.clearcoatRoughness = 0.03;
        mat.iridescence = 0.85;
        mat.iridescenceIOR = 1.8;
        mat.emissive.set('#010806');
        mat.transparent = false;
        mat.opacity = 1.0;
        mat.map = mesh.userData['originalMap'];
        mat.normalMap = mesh.userData['originalNormalMap'];
        mat.roughnessMap = mesh.userData['originalRoughnessMap'];
        mat.metalnessMap = mesh.userData['originalMetalnessMap'];
      }
      mat.needsUpdate = true;
    });
  }

  private loadModel(path: string): void {
    const gltfLoader = new GLTFLoader();
    gltfLoader.setMeshoptDecoder(MeshoptDecoder);

    gltfLoader.load(
      path,
      (asset) => {
        const modelMesh = asset.scene;

        const box = new THREE.Box3().setFromObject(modelMesh);
        const size = box.getSize(new THREE.Vector3());
        const center = box.getCenter(new THREE.Vector3());
        modelMesh.position.sub(center);

        const maxDim = Math.max(size.x, size.y, size.z);
        const scale = 2.1 / maxDim;
        this.baseScale = scale;
        modelMesh.scale.setScalar(scale);

        modelMesh.traverse((child: any) => {
          if (child.isMesh) {
            child.castShadow = false;
            child.receiveShadow = false;

            const oldMat = Array.isArray(child.material) ? child.material[0] : child.material;
            
            const newMat = new THREE.MeshPhysicalMaterial({
              color: new THREE.Color('#031210'),
              metalness: 0.98,
              roughness: 0.08,
              transmission: 0.0,
              ior: 1.5,
              thickness: 0.0,
              clearcoat: 1.0,
              clearcoatRoughness: 0.03,
              iridescence: 0.85,
              iridescenceIOR: 1.8,
              iridescenceThicknessRange: [100, 350],
              emissive: new THREE.Color('#010806'),
              transparent: false,
              opacity: 1.0,
              map: oldMat ? oldMat.map : null,
              normalMap: oldMat ? oldMat.normalMap : null,
              roughnessMap: oldMat ? oldMat.roughnessMap : null,
              metalnessMap: oldMat ? oldMat.metalnessMap : null
            });

            newMat.onBeforeCompile = (shader) => {
              shader.uniforms['u_waveProgress'] = { value: 0.0 };
              shader.uniforms['u_waveActive'] = { value: 0.0 };
              shader.uniforms['u_intersectPoint'] = { value: new THREE.Vector3(0, 0, 0) };
              newMat.userData['shader'] = shader;
              this.waveShaders.push(shader);

              shader.vertexShader = `
                uniform float u_waveProgress;
                uniform float u_waveActive;
                uniform vec3  u_intersectPoint;
              ` + shader.vertexShader;
              
              shader.vertexShader = shader.vertexShader.replace(
                '#include <begin_vertex>',
                `
                #include <begin_vertex>
                if (u_waveActive > 0.5) {
                  float distToHit  = length(position - u_intersectPoint);
                  float waveRadius = u_waveProgress * 2.5;
                  float d          = distToHit - waveRadius;
                  float wave       = exp(-d * d * 5.0);
                  float ripple     = sin(d * 5.0 - u_waveProgress * 6.28) * 0.038 * wave;
                  transformed.z   += ripple;
                }
                `
              );
            };

            // Cache original texture maps
            child.userData['originalMap'] = oldMat ? oldMat.map : null;
            child.userData['originalNormalMap'] = oldMat ? oldMat.normalMap : null;
            child.userData['originalRoughnessMap'] = oldMat ? oldMat.originalRoughnessMap : null;
            child.userData['originalMetalnessMap'] = oldMat ? oldMat.originalMetalnessMap : null;

            child.material = newMat;
            if (oldMat) oldMat.dispose();

            this.meshes.push(child);
          }
        });

        this.modelGroup.add(modelMesh);
        this.modelLoading = false;

        // Apply correct initial theme state
        this.applyThemeToModel(this.isLightTheme);

        if (this.canvas3d) {
          this.canvas3d.style.opacity = '0';
          this.canvas3d.style.transition = 'opacity 1.5s ease-out';
          requestAnimationFrame(() => { 
            if (this.canvas3d) this.canvas3d.style.opacity = '1'; 
          });
        }
      },
      (progress) => {
        const total = progress.total || progress.loaded || 1;
        const percent = Math.round((progress.loaded / total) * 100);
        console.log(`Loading T-Reserve Core Model: ${percent}%`);
      },
      (error) => {
        console.error('GLB load error:', error);
        this.modelLoading = false;
      }
    );
  }

  private updateSceneTheme(isLight: boolean): void {
    if (!this.scene) return;

    if (isLight) {
      this.ambientLight.color.set('#f4f3ea'); this.ambientLight.intensity = 0.95;
      this.cyanDirLight.color.set('#00ffc8'); this.cyanDirLight.intensity = 11.5;
      this.purpleDirLight.color.set('#e054ff'); this.purpleDirLight.intensity = 9.5;
      this.cyanSoftLight.color.set('#00ffc8'); this.cyanSoftLight.intensity = 4.0;
      this.whiteRimLight.intensity = 4.5;
      this.waveLight.color.set('#bdffd9');
      this.cursorLight.color.set('#fad4ff');
      this.colorCyan.set('#72ccb4');
      this.colorPurple.set('#e2bdff');
      this.colorMagenta.set('#ffbde2');
      this.emissiveIdle.set('#020504');
      this.emissiveHover.set('#05070a');
    } else {
      this.ambientLight.color.set('#010705'); this.ambientLight.intensity = 1.2;
      this.cyanDirLight.color.set('#00ffcc'); this.cyanDirLight.intensity = 6.5;
      this.purpleDirLight.color.set('#b82cff'); this.purpleDirLight.intensity = 5.5;
      this.cyanSoftLight.color.set('#00ffcc'); this.cyanSoftLight.intensity = 2.8;
      this.whiteRimLight.intensity = 2.5;
      this.waveLight.color.set('#00ffcc');
      this.cursorLight.color.set('#00ffcc');
      this.colorCyan.set('#00ffcc');
      this.colorPurple.set('#a85cfc');
      this.colorMagenta.set('#ff00aa');
      this.emissiveIdle.set('#021209');
      this.emissiveHover.set('#011c16');
    }

    // Apply theme directly
    this.applyThemeToModel(isLight);

    // If model not loaded, trigger load
    if (this.modelGroup.children.length === 0 && !this.modelLoading) {
      this.modelLoading = true;
      this.loadModel('/core.glb');
    }
  }

  private tick = (now = performance.now()): void => {
    if (this.isDestroyed) return;

    try {
      this.tickFrame++;
      const elapsed = (now - this.startTime) * 0.001;

      // Smooth linear interpolation (lerp) for tilts
      this.mouseX += (this.targetMouseX - this.mouseX) * 0.25;
      this.mouseY += (this.targetMouseY - this.mouseY) * 0.25;

      // Project 2D cursor into 3D space
      this.tempV.set(this.mouseVector.x, this.mouseVector.y, 0.5);
      this.tempV.unproject(this.camera);
      
      // Project ray direction onto Z=0.5 plane
      this.rayDir.copy(this.tempV).sub(this.camera.position).normalize();
      const planeDistance = (0.5 - this.camera.position.z) / this.rayDir.z;
      this.projectedCursor3D.copy(this.camera.position).addScaledVector(this.rayDir, planeDistance);

      const mouseSpeed = Math.sqrt(this.mouseVelX * this.mouseVelX + this.mouseVelY * this.mouseVelY);

      const isScrolled = window.scrollY > 150;

      // Engine starting effect
      if (this.engineStarting) {
        this.engineStartTimer += 0.016; // approx 60fps delta
        
        // Spin the model crazily
        this.spinGroup.rotation.y += 0.4 * (this.engineStartTimer * 5);
        this.spinGroup.rotation.x += (Math.random() - 0.5) * 0.1;
        
        // Intense core glow
        this.innerCyanGlow.intensity = 50.0 + Math.sin(elapsed * 60.0) * 30.0;
        
        // Crazy particle emissions from core
        for(let j=0; j<5; j++) {
           this.emitStardust(new THREE.Vector3(0,0,0), (Math.random()-0.5)*15, (Math.random()-0.5)*15);
        }
        
        // Zoom camera in dramatically
        this.camera.position.z += (2.0 - this.camera.position.z) * 0.05;

        if (this.engineStartTimer > 1.8) {
          this.engineStarting = false;
        }
      } else if (this.processing) {
        this.processingTimer += 0.016;
        const t = this.processingTimer;
        let torque = 0;
        
        // Engine "Rev" Sequence: tun-tun tun-tuuuun tun-tun-tun
        if (t > 0.0 && t < 0.25) {
           const local = t / 0.25;
           torque = Math.sin(local * Math.PI) * 0.8;
        } else if (t > 0.35 && t < 0.6) {
           const local = (t - 0.35) / 0.25;
           torque = Math.sin(local * Math.PI) * 0.8;
        } else if (t > 0.65 && t < 1.15) {
           const local = (t - 0.65) / 0.5;
           torque = Math.sin(local * Math.PI) * 1.8; // Heavy pull
        } else if (t > 1.2 && t < 1.8) {
           // Bouncing rev limiter
           torque = Math.abs(Math.sin((t - 1.2) * Math.PI * 7.0)) * 1.4;
        }

        // Exponential smoothing for heavy mechanical weight
        this.smoothedTorque += (torque - this.smoothedTorque) * 0.35;
        
        const baseY = Math.PI / 2 + Math.PI;
        const baseX = -10 * (Math.PI / 180);

        // Twisting the engine on its mounts
        this.spinGroup.rotation.y = baseY + this.smoothedTorque * 0.25;
        this.spinGroup.rotation.x = baseX - this.smoothedTorque * 0.1;
        this.spinGroup.rotation.z = this.smoothedTorque * 0.3;
        
        // Extreme positional vibration ONLY when torque is applied
        const vibeStr = this.smoothedTorque * 0.05;
        this.spinGroup.position.x = (Math.random() - 0.5) * vibeStr;
        this.spinGroup.position.y = (Math.random() - 0.5) * vibeStr;

        // Core glow flares up aggressively with the revs
        this.innerCyanGlow.intensity = 15.0 + this.smoothedTorque * 50.0;
        
        // Spit sparks out of the core on high torque!
        if (torque > 0.5 && Math.random() > 0.4) {
          this.emitStardust(new THREE.Vector3(0, 0, 0), (Math.random()-0.5)*10, (Math.random()-0.5)*10);
        }

        // Camera punches inward slightly on heavy revs
        const targetZ = 4.0 - this.smoothedTorque * 0.4;
        this.camera.position.z += (targetZ - this.camera.position.z) * 0.15;

        if (this.processingTimer > 1.8) {
          this.processing = false;
          this.smoothedTorque = 0;
          this.spinGroup.position.set(0, 0, 0); // Reset position shake
          this.engineIdling = true;
          this.idleTimer = 0;
        }
      } else if (this.engineIdling) {
        this.idleTimer += 0.016;
        
        // Fast, light vibration (engine idling fast)
        const t = this.idleTimer * 60.0;
        this.spinGroup.rotation.y = (Math.PI / 2 + Math.PI) + Math.sin(t * 1.5) * 0.02;
        this.spinGroup.rotation.x = -10 * (Math.PI / 180) + Math.cos(t * 1.8) * 0.02;
        
        this.spinGroup.position.x = Math.sin(t * 2.1) * 0.01;
        this.spinGroup.position.y = Math.cos(t * 2.5) * 0.01;
        
        // Keep core glow slightly elevated
        this.innerCyanGlow.intensity = 18.0 + Math.sin(elapsed * 40.0) * 5.0;
        
        // Restore camera
        this.camera.position.z += (4.0 - this.camera.position.z) * 0.05;

        // Idle for 4 seconds while scrolling happens
        if (this.idleTimer > 4.0) {
           this.engineIdling = false;
           this.spinGroup.position.set(0, 0, 0);
        }
      } else {
        // Restore camera
        this.camera.position.z += (4.0 - this.camera.position.z) * 0.05;

        // Set fixed baseline front views smoothly
        this.spinGroup.rotation.x += (-10 * (Math.PI / 180) - this.spinGroup.rotation.x) * 0.1;
        
        // Normalize rotation Y back to baseline
        const targetRotY = Math.PI / 2 + Math.PI;
        const diff = (targetRotY - this.spinGroup.rotation.y) % (Math.PI * 2);
        this.spinGroup.rotation.y += (diff < -Math.PI ? diff + Math.PI * 2 : (diff > Math.PI ? diff - Math.PI * 2 : diff)) * 0.1;

        // Restore Z rotation smoothly
        this.spinGroup.rotation.z += (0 - this.spinGroup.rotation.z) * 0.1;
      }

      // Emit sparkling stardust particles
      if (elapsed > 1.0 && !isScrolled && !this.engineStarting) {
        this.emitStardust(this.projectedCursor3D, this.mouseVelX, this.mouseVelY);
        
        if (mouseSpeed > 0.003) {
          const extraSparks = Math.min(Math.floor(mouseSpeed * 320), 6);
          for (let k = 0; k < extraSparks; k++) {
            const ratio = k / extraSparks;
            this.interpOffset.set(-this.mouseVelX, -this.mouseVelY, 0);
            this.interpPos.copy(this.projectedCursor3D).addScaledVector(this.interpOffset, ratio);
            this.emitStardust(this.interpPos, this.mouseVelX, this.mouseVelY);
          }
        }
      }

      // Decay mouse velocity naturally over time
      this.mouseVelX *= 0.88;
      this.mouseVelY *= 0.88;

      // Update existing particles in the stardust pool
      const posAttr = this.pGeometry.attributes['position'] as THREE.BufferAttribute;
      const sizeAttr = this.pGeometry.attributes['aSize'] as THREE.BufferAttribute;
      const alphaAttr = this.pGeometry.attributes['aAlpha'] as THREE.BufferAttribute;
      
      const positionsArr = posAttr.array as Float32Array;
      const sizesArr = sizeAttr.array as Float32Array;
      const alphasArr = alphaAttr.array as Float32Array;

      for (let i = 0; i < this.particleCount; i++) {
        const p = this.particles[i];
        if (p.active) {
          p.age++;
          
          p.pos.add(p.vel);
          p.pos.x += Math.sin(elapsed * 1.8 + i) * 0.0018 - 0.0004;
          p.pos.y += 0.0008;

          this.particlePull.copy(p.pos).multiplyScalar(-1).normalize();
          p.vel.addScaledVector(this.particlePull, 0.0002);
          
          p.vel.multiplyScalar(0.96);

          positionsArr[i * 3] = p.pos.x;
          positionsArr[i * 3 + 1] = p.pos.y;
          positionsArr[i * 3 + 2] = p.pos.z;

          const t = p.age / p.life;
          sizesArr[i] = p.size * (1.0 - t);
          alphasArr[i] = p.alpha * (1.0 - t * t);

          if (p.age >= p.life) {
            p.active = false;
            positionsArr[i * 3] = 999.0;
            positionsArr[i * 3 + 1] = 999.0;
            positionsArr[i * 3 + 2] = 999.0;
            sizesArr[i] = 0.0;
            alphasArr[i] = 0.0;
          }
        }
      }
      posAttr.needsUpdate = true;
      sizeAttr.needsUpdate = true;
      alphaAttr.needsUpdate = true;

      const maxTilt = 30 * (Math.PI / 180);
      const targetTiltX = this.mouseY * maxTilt;
      const targetTiltY = this.mouseX * maxTilt;

      this.tiltGroup.rotation.x += (targetTiltX - this.tiltGroup.rotation.x) * 0.25;
      this.tiltGroup.rotation.y += (targetTiltY - this.tiltGroup.rotation.y) * 0.25;

      // Raycast proximity
      if (this.modelGroup.children.length > 0) {
        if (isScrolled) {
          this.isHovered = false;
        } else if (this.mouseDirty && this.tickFrame % 3 === 0) {
          this.mouseDirty = false;
          this.raycaster.setFromCamera(this.mouseVector, this.camera);
          const intersects = this.raycaster.intersectObjects([this.hitSphere], false);
          this.isHovered = intersects.length > 0;
          if (this.isHovered) {
            this.lastHitPoint.copy(intersects[0].point);
            if (intersects[0].face) {
              this.lastHitNormal.copy(intersects[0].face.normal);
            }
            this.lastHitObject = intersects[0].object;
          }
        }

        if (this.isHovered && this.lastHitObject) {
          this.hitPoint.copy(this.lastHitPoint);
          this.hitNormal.copy(this.lastHitNormal);
          
          this.normalMatrix.getNormalMatrix(this.lastHitObject.matrixWorld);
          this.hitNormal.applyMatrix3(this.normalMatrix).normalize();
          
          // Color cycles
          const cycle = (elapsed * 1.5) % 3.0;
          if (cycle < 1.0) {
            this.currentColor.lerpColors(this.colorCyan, this.colorPurple, cycle);
          } else if (cycle < 2.0) {
            this.currentColor.lerpColors(this.colorPurple, this.colorMagenta, cycle - 1.0);
          } else {
            this.currentColor.lerpColors(this.colorMagenta, this.colorCyan, cycle - 2.0);
          }
          
          this.lightPos.copy(this.hitPoint).addScaledVector(this.hitNormal, 0.22);
          this.cursorLight.position.copy(this.lightPos);
          
          this.cursorLight.intensity = 45.0;
          this.cursorLight.distance = 2.5;
          this.cursorLight.color.copy(this.currentColor);
          
          this.sparkPos.copy(this.hitPoint).addScaledVector(this.hitNormal, 0.02);
          this.sparkSprite.position.copy(this.sparkPos);
          
          const pulse = 0.26 + Math.sin(elapsed * 15.0) * 0.05;
          this.sparkSprite.scale.set(pulse, pulse, 1.0);
          this.sparkSprite.material.color.copy(this.currentColor);
        } else {
          this.projectedCursor3D.z = Math.max(this.projectedCursor3D.z, 1.6);
          
          this.cursorLight.position.lerp(this.projectedCursor3D, 0.15);
          
          // Fade out the light smoothly when scrolled
          const fade = Math.max(0, 1.0 - window.scrollY / 200.0);
          this.cursorLight.intensity = 6.0 * fade;
          this.cursorLight.distance = 4.0;
          this.cursorLight.color.set('#00a892');
          
          this.sparkSprite.scale.set(0.0, 0.0, 1.0);
        }

        this.lastHoverState = this.isHovered;
      }

      let targetScale = 1.0;
      if (this.isHovered) {
        targetScale = 1.03; 
      }

      this.hoverScale += (targetScale - this.hoverScale) * 0.08;
      this.modelGroup.scale.setScalar(this.hoverScale);

      // Emissive update
      for (let i = 0; i < this.meshes.length; i++) {
        const mat = this.meshes[i].material as THREE.MeshPhysicalMaterial;
        if (!mat) continue;

        if (this.isHovered && this.waveActive) {
          mat.emissive.lerpColors(this.emissiveHover, this.currentColor, 0.08);
        } else if (this.isHovered) {
          mat.emissive.lerp(this.emissiveHover, 0.1);
        } else {
          mat.emissive.lerp(this.emissiveIdle, 0.05);
        }
      }

      // Wave sweep
      if (this.waveActive) {
        this.waveProgress += 0.016;
        
        if (this.waveProgress > 1.0) {
          this.waveActive = false;
          this.waveLight.intensity = 0.0;
          
          for (let i = 0; i < this.meshes.length; i++) {
            const mat = this.meshes[i].material as any;
            const sh = mat?.userData?.['shader'];
            if (sh) {
              sh.uniforms['u_waveActive'].value = 0.0;
              sh.uniforms['u_waveProgress'].value = 0.0;
            }
          }
        } else {
          const waveY = 1.8 - this.waveProgress * 3.6;
          this.waveLight.position.set(0, waveY, 0.4);
          this.waveLight.intensity = 18.0 * Math.sin(this.waveProgress * Math.PI);
          this.waveLight.color.lerpColors(this.waveColorStart, this.waveColorEnd, this.waveProgress);
          
          for (let i = 0; i < this.meshes.length; i++) {
            const mat = this.meshes[i].material as any;
            const sh = mat?.userData?.['shader'];
            if (sh) {
              sh.uniforms['u_waveActive'].value = 1.0;
              sh.uniforms['u_waveProgress'].value = this.waveProgress;
            }
          }
        }
      }

      this.tiltGroup.position.y = -0.15 + Math.sin(elapsed * 1.5) * 0.07;
      this.innerCyanGlow.intensity = 12.0 + Math.sin(elapsed * 2.8) * 6.5;

      this.cyanDirLight.position.x = -3.0 - this.mouseX * 2.0;
      this.cyanDirLight.position.y = -1.8 - this.mouseY * 2.0;
      this.purpleDirLight.position.x = 3.0 + this.mouseX * 1.5;
      this.purpleDirLight.position.y = -0.5 + this.mouseY * 1.5;

      this.renderer.render(this.scene, this.camera);
      this.animationFrameId = requestAnimationFrame(this.tick);
    } catch (e) {
      console.error("[DEBUG] TICK CRASHED:", e);
    }
  };

  /* ==========================================================================
     2. SMOOTH INERTIAL CURSOR GLOW WIDGET
     ========================================================================== */
  private initCursorGlow(): void {
    const glow = this.mouseGlowEl;
    if (!glow) return;

    let glowMouseX = window.innerWidth / 2;
    let glowMouseY = window.innerHeight / 2;
    let currentX = glowMouseX;
    let currentY = glowMouseY;
    const speed = 0.35; // Beautiful organic delay from Motion Design

    this.addListener(document, 'mousemove', (e: MouseEvent) => {
      glowMouseX = e.clientX;
      glowMouseY = e.clientY;
    });

    const updateGlowPosition = (): void => {
      if (this.isDestroyed || !glow) return;
      currentX += (glowMouseX - currentX) * speed;
      currentY += (glowMouseY - currentY) * speed;

      glow.style.left = `${currentX}px`;
      glow.style.top = `${currentY}px`;

      this.mobileNodesAnimationFrameId = requestAnimationFrame(updateGlowPosition);
    };

    updateGlowPosition();

    this.addListener(document, 'mousedown', () => {
      if (glow) {
        glow.style.width = '450px';
        glow.style.height = '450px';
      }
    });

    this.addListener(document, 'mouseup', () => {
      if (glow) {
        glow.style.width = '320px';
        glow.style.height = '320px';
      }
    });
  }

  /* ==========================================================================
     2.5. HIGH-DENSITY SCREEN-SPACE GLASS SCRATCH SPARKLER
     ========================================================================== */


  /* ==========================================================================
     5. MOBILE 2D NODES CANVAS (Fallback for weak/mobile devices)
     ========================================================================== */
  private initMobileNodes(): void {
    const canvas = this.canvasMobile;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    
    let width = canvas.offsetWidth;
    let height = canvas.offsetHeight;
    canvas.width = width * window.devicePixelRatio;
    canvas.height = height * window.devicePixelRatio;
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio);

    const updateMouse = (x: number, y: number): void => {
      const rect = canvas.getBoundingClientRect();
      this.mobileMouse.x = x - rect.left;
      this.mobileMouse.y = y - rect.top;
    };
    
    this.addListener(canvas, 'mousemove', (e: MouseEvent) => updateMouse(e.clientX, e.clientY));
    this.addListener(canvas, 'touchmove', (e: TouchEvent) => {
      if(e.touches.length > 0) {
        updateMouse(e.touches[0].clientX, e.touches[0].clientY);
      }
    }, { passive: true });
    
    this.addListener(canvas, 'mouseleave', () => { this.mobileMouse.x = -1000; this.mobileMouse.y = -1000; });
    this.addListener(canvas, 'touchend', () => { this.mobileMouse.x = -1000; this.mobileMouse.y = -1000; });

    class MobileParticle {
      public x: number;
      public y: number;
      public vx: number;
      public vy: number;
      public baseRadius: number;

      constructor() {
        this.x = Math.random() * width;
        this.y = Math.random() * height;
        this.vx = (Math.random() - 0.5) * 1.5;
        this.vy = (Math.random() - 0.5) * 1.5;
        this.baseRadius = 1.5 + Math.random() * 2;
      }

      public update(mouseState: any): void {
        this.x += this.vx;
        this.y += this.vy;

        if (this.x < 0 || this.x > width) this.vx *= -1;
        if (this.y < 0 || this.y > height) this.vy *= -1;

        const dx = mouseState.x - this.x;
        const dy = mouseState.y - this.y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < mouseState.radius) {
          const forceDirectionX = dx / dist;
          const forceDirectionY = dy / dist;
          const force = (mouseState.radius - dist) / mouseState.radius;
          this.x -= forceDirectionX * force * 5;
          this.y -= forceDirectionY * force * 5;
        }
      }

      public draw(context: CanvasRenderingContext2D, isLight: boolean): void {
        context.beginPath();
        context.arc(this.x, this.y, this.baseRadius, 0, Math.PI * 2);
        context.fillStyle = isLight ? '#a85cfc' : '#00ffcc';
        context.fill();
      }
    }

    for (let i = 0; i < this.mobileParticleCount; i++) {
      this.mobileParticles.push(new MobileParticle());
    }

    const animate = (): void => {
      if (this.isDestroyed || !ctx) return;
      this.mobileNodesAnimationFrameId = requestAnimationFrame(animate);
      
      // Only animate if mobile nodes canvas is actually visible
      if (window.innerWidth >= 768) return;
      
      ctx.clearRect(0, 0, width, height);

      for (let i = 0; i < this.mobileParticles.length; i++) {
        this.mobileParticles[i].update(this.mobileMouse);
        this.mobileParticles[i].draw(ctx, this.isLightTheme);
        
        for (let j = i; j < this.mobileParticles.length; j++) {
          const dx = this.mobileParticles[i].x - this.mobileParticles[j].x;
          const dy = this.mobileParticles[i].y - this.mobileParticles[j].y;
          const distance = Math.sqrt(dx * dx + dy * dy);
          
          if (distance < this.mobileMaxDistance) {
            ctx.beginPath();
            ctx.strokeStyle = this.isLightTheme 
              ? `rgba(168, 92, 252, ${1 - distance/this.mobileMaxDistance})` 
              : `rgba(0, 255, 204, ${1 - distance/this.mobileMaxDistance})`;
            ctx.lineWidth = 1;
            ctx.moveTo(this.mobileParticles[i].x, this.mobileParticles[i].y);
            ctx.lineTo(this.mobileParticles[j].x, this.mobileParticles[j].y);
            ctx.stroke();
          }
        }
      }
    };
    
    animate();

    this.addListener(window, 'resize', () => {
      width = canvas.offsetWidth;
      height = canvas.offsetHeight;
      if (width > 0 && height > 0) {
        canvas.width = width * window.devicePixelRatio;
        canvas.height = height * window.devicePixelRatio;
        ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
      }
    });
  }
}
