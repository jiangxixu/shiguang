const fs=require('node:fs'),path=require('node:path'),esbuild=require('esbuild');
const base=path.join(__dirname,'..');
esbuild.buildSync({stdin:{contents:"export {createClient} from '@supabase/supabase-js'",resolveDir:base,loader:'js'},bundle:true,format:'iife',globalName:'ShiguangSupabase',target:['chrome100'],minify:true,outfile:path.join(base,'dist/supabase.js'),legalComments:'eof'});
for(const name of ['core.js','capture-store.js','cloud-config.js','auth-input.js','supabase.js'])fs.copyFileSync(path.join(base,'dist',name),path.join(base,'collector',name));
console.log('Supabase client bundled locally.');
