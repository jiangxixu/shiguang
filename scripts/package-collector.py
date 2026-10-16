"""Build or verify the downloadable extension from the exact checked-in sources."""
from pathlib import Path
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
files = sorted(path for path in (root / 'collector').iterdir() if path.is_file())
archive = root / 'dist' / 'collector.zip'
if '--check' in sys.argv:
    with zipfile.ZipFile(archive) as bundle:
        assert set(bundle.namelist()) == {path.name for path in files}, 'Extension file list differs'
        for path in files:
            # Git may normalize working-tree line endings on different hosts.
            assert bundle.read(path.name).replace(b'\r\n', b'\n') == path.read_bytes().replace(b'\r\n', b'\n'), path.name
    print('Downloadable extension matches source files.')
else:
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as bundle:
        for path in files:
            info = zipfile.ZipInfo(path.name, (2026, 10, 16, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            bundle.writestr(info, path.read_bytes().replace(b'\r\n', b'\n'))
    print('Extension packaged.')
